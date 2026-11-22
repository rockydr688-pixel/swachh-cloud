package com.cloudclean.scanner;

import com.cloudclean.analysis.ImageAnalysisService;
import com.cloudclean.detection.*;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.dto.ScanJobResponse;
import com.cloudclean.dto.ScanProgressEvent;
import com.cloudclean.dto.ScanRequest;
import com.cloudclean.exception.BadRequestException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.file.FileItem;
import com.cloudclean.file.FileService;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.ScanJobRepository;
import com.cloudclean.repository.StorageConnectionRepository;
import com.cloudclean.storage.StorageConnection;
import com.cloudclean.storage.agent.LocalAgentScanner;
import com.cloudclean.storage.provider.ListFilesOptions;
import com.cloudclean.storage.provider.ProviderFileMetadata;
import com.cloudclean.storage.provider.StorageProvider;
import com.cloudclean.storage.provider.StorageProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ScanJobRepository scanJobRepository;
    private final FileItemRepository fileItemRepository;
    private final StorageConnectionRepository connectionRepository;
    private final StorageProviderRegistry storageProviderRegistry;
    private final LocalAgentScanner localAgentScanner;
    private final FileService fileService;
    private final DuplicateDetector duplicateDetector;
    private final BlurDetector blurDetector;
    private final LargeFileDetector largeFileDetector;
    private final InactiveFileDetector inactiveFileDetector;
    private final TemporaryFileDetector temporaryFileDetector;
    private final JunkScoringEngine junkScoringEngine;
    private final com.cloudclean.notification.NotificationService notificationService;

    // In-memory cancellation registry for active scan jobs
    private final Map<String, AtomicBoolean> activeScanCancellations = new ConcurrentHashMap<>();

    // Real-time Server-Sent Events subscribers per scan jobId
    private final Map<String, List<SseEmitter>> scanEmitters = new ConcurrentHashMap<>();

    public ScanService(ScanJobRepository scanJobRepository,
                       FileItemRepository fileItemRepository,
                       StorageConnectionRepository connectionRepository,
                       StorageProviderRegistry storageProviderRegistry,
                       LocalAgentScanner localAgentScanner,
                       FileService fileService,
                       DuplicateDetector duplicateDetector,
                       BlurDetector blurDetector,
                       LargeFileDetector largeFileDetector,
                       InactiveFileDetector inactiveFileDetector,
                       TemporaryFileDetector temporaryFileDetector,
                       JunkScoringEngine junkScoringEngine,
                       com.cloudclean.notification.NotificationService notificationService) {
        this.scanJobRepository = scanJobRepository;
        this.fileItemRepository = fileItemRepository;
        this.connectionRepository = connectionRepository;
        this.storageProviderRegistry = storageProviderRegistry;
        this.localAgentScanner = localAgentScanner;
        this.fileService = fileService;
        this.duplicateDetector = duplicateDetector;
        this.blurDetector = blurDetector;
        this.largeFileDetector = largeFileDetector;
        this.inactiveFileDetector = inactiveFileDetector;
        this.temporaryFileDetector = temporaryFileDetector;
        this.junkScoringEngine = junkScoringEngine;
        this.notificationService = notificationService;
    }

    /**
     * Subscribe to live SSE scan progress updates.
     */
    public SseEmitter subscribeToScanProgress(String jobId, String userId) {
        ScanJob job = scanJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan job not found with id: " + jobId));

        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L); // 15 min timeout
        scanEmitters.computeIfAbsent(jobId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(jobId, emitter));
        emitter.onTimeout(() -> removeEmitter(jobId, emitter));
        emitter.onError(e -> removeEmitter(jobId, emitter));

        // Immediately push initial event snapshot
        ScanProgressEvent initialEvent = createProgressEventFromJob(job);
        try {
            emitter.send(SseEmitter.event().name("scan-progress").data(initialEvent));
        } catch (IOException e) {
            removeEmitter(jobId, emitter);
        }

        return emitter;
    }

    private void removeEmitter(String jobId, SseEmitter emitter) {
        List<SseEmitter> list = scanEmitters.get(jobId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                scanEmitters.remove(jobId);
            }
        }
    }

    public void broadcastProgress(String jobId, int progressPercent, String status, String stage,
                                  String currentProvider, String activeDir, int discovered, int analyzed,
                                  int duplicates, int blurry, int large, int inactive, int temporary,
                                  int failed, long bytes, long savings, String error) {
        ScanProgressEvent event = new ScanProgressEvent();
        event.setJobId(jobId);
        event.setStatus(status);
        event.setProgressPercent(progressPercent);
        event.setStage(stage);
        event.setCurrentProvider(currentProvider);
        event.setActiveDirectory(activeDir);
        event.setFilesDiscovered(discovered);
        event.setFilesAnalyzed(analyzed);
        event.setDuplicatesDetected(duplicates);
        event.setBlurryImagesDetected(blurry);
        event.setLargeFilesDetected(large);
        event.setInactiveFilesDetected(inactive);
        event.setTemporaryFilesDetected(temporary);
        event.setFailedFilesCount(failed);
        event.setBytesScanned(bytes);
        event.setPotentialSavingsBytes(savings);
        event.setErrorMessage(error);

        List<SseEmitter> list = scanEmitters.get(jobId);
        if (list != null) {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("scan-progress").data(event));
                    if ("completed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status)) {
                        emitter.complete();
                    }
                } catch (Exception ex) {
                    removeEmitter(jobId, emitter);
                }
            }
        }
    }

    private ScanProgressEvent createProgressEventFromJob(ScanJob job) {
        ScanProgressEvent event = new ScanProgressEvent();
        event.setJobId(job.getId());
        event.setStatus(job.getStatus());
        event.setProgressPercent(job.getProgressPercent());
        event.setCurrentProvider(job.getStorageType());
        event.setActiveDirectory(job.getActiveDirectory());
        event.setFilesDiscovered(job.getTotalFilesScanned());
        event.setFilesAnalyzed(job.getTotalFilesScanned());
        event.setFailedFilesCount(job.getFailedFilesCount());
        event.setBytesScanned(job.getTotalBytesScanned());
        event.setPotentialSavingsBytes(job.getPotentialSavingsBytes());
        event.setDuration(job.getDuration());
        event.setErrorMessage(job.getErrorMessage());
        event.setStage("completed".equalsIgnoreCase(job.getStatus()) ? "COMPLETED" : "IN_PROGRESS");
        return event;
    }

    /**
     * Triggers a new scan with duplicate scan prevention and validation.
     */
    public ScanJobResponse triggerScan(String userId, ScanRequest request) {
        String target = request.getTarget() != null ? request.getTarget() : "Local Computer";
        String storageType = request.getStorageType() != null ? request.getStorageType() : "local";

        // 1. Duplicate scan prevention
        List<ScanJob> activeJobs = scanJobRepository.findByUserIdAndTargetAndStatusIn(
                userId, target, List.of("in_progress", "queued")
        );
        if (!activeJobs.isEmpty()) {
            throw new BadRequestException("An active scan is already in progress for target: '" + target +
                    "'. Please wait for it to complete or cancel it before starting a new scan.");
        }

        // 2. Resolve storage connection
        StorageConnection connection = resolveStorageConnection(userId, request);

        // 3. Create initial ScanJob record
        ScanJob job = new ScanJob(userId, target, storageType);
        job.setStatus("queued");
        job.setProgressPercent(5);
        job.setStartTime(Instant.now());
        if (connection != null) {
            job.setStorageConnectionId(connection.getId());
        }
        job.setIncremental(request.isIncremental());

        ScanJob saved = scanJobRepository.save(job);
        activeScanCancellations.put(saved.getId(), new AtomicBoolean(false));
        log.info("Enqueued scan job id: {} on target: {} for user: {}", saved.getId(), target, userId);

        // 4. Asynchronously execute the scan job
        runScanJobAsync(saved.getId(), userId, request, connection);

        return new ScanJobResponse(saved);
    }

    @Async("scanTaskExecutor")
    public void runScanJobAsync(String jobId, String userId, ScanRequest request, StorageConnection connection) {
        Instant startTime = Instant.now();
        AtomicBoolean cancellationFlag = activeScanCancellations.computeIfAbsent(jobId, k -> new AtomicBoolean(false));

        String storageType = request.getStorageType() != null ? request.getStorageType().toLowerCase() : "local";
        if (connection != null && connection.getType() != null) {
            storageType = connection.getType().toLowerCase();
        }

        try {
            log.info("Starting asynchronous processing for scan job: {} on {}", jobId, storageType);
            updateJob(jobId, 10, "in_progress", "Initializing scanner...", 0, 0, 0L);
            broadcastProgress(jobId, 10, "in_progress", "INITIALIZING", storageType, "Initializing scanner...",
                    0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L, null);

            ListFilesOptions options = new ListFilesOptions();
            options.setRecursive(true);

            // Incremental scan support
            if (request.isIncremental()) {
                scanJobRepository.findFirstByUserIdAndTargetAndStatusOrderByCompletedTimeDesc(
                        userId, request.getTarget(), "completed"
                ).ifPresent(lastCompleted -> {
                    if (lastCompleted.getCompletedTime() != null) {
                        options.setModifiedSince(lastCompleted.getCompletedTime());
                        log.info("Running incremental scan on {} since: {}", request.getTarget(), lastCompleted.getCompletedTime());
                    }
                });
            }

            List<ProviderFileMetadata> discoveredFiles = new ArrayList<>();
            int failedCount = 0;
            long totalBytes = 0L;

            if ("local".equalsIgnoreCase(storageType)) {
                List<String> monitoredPaths = connection != null && connection.getMonitoredPaths() != null && !connection.getMonitoredPaths().isEmpty()
                        ? connection.getMonitoredPaths()
                        : List.of(System.getProperty("user.home") + "/Downloads");

                final String st = storageType;
                discoveredFiles = localAgentScanner.scanDirectories(
                        monitoredPaths,
                        options,
                        cancellationFlag::get,
                        progress -> {
                            int pct = 10 + (int) Math.min(25, (progress.filesScanned() / 10.0));
                            updateJob(jobId, pct, "in_progress", progress.currentDirectory(),
                                    progress.filesScanned(), progress.failedFiles(), progress.totalBytes());
                            broadcastProgress(jobId, pct, "in_progress", "DISCOVERING_FILES", st,
                                    progress.currentDirectory(), progress.filesScanned(), 0, 0, 0, 0, 0, 0,
                                    progress.failedFiles(), progress.totalBytes(), 0L, null);
                        }
                );
            } else if (storageProviderRegistry.hasProvider(storageType)) {
                if (connection == null || connection.getEncryptedAccessToken() == null) {
                    throw new BadRequestException("Storage connection for " + storageType + " is not connected or authenticated.");
                }
                StorageProvider provider = storageProviderRegistry.getProvider(storageType);
                broadcastProgress(jobId, 15, "in_progress", "DISCOVERING_FILES", storageType,
                        "Connecting to " + provider.getProviderType() + " API...",
                        0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L, null);
                discoveredFiles = provider.listFiles(connection.getId(), options);
                log.info("Discovered {} files from provider {}", discoveredFiles.size(), storageType);
            } else {
                throw new BadRequestException("Unsupported storage type for scanning: " + storageType);
            }

            for (ProviderFileMetadata meta : discoveredFiles) {
                totalBytes += meta.getSizeBytes();
            }

            if (cancellationFlag.get()) {
                handleCancellation(jobId, startTime, storageType);
                return;
            }

            // Ingest discovered files into MongoDB (incremental upsert)
            if (!discoveredFiles.isEmpty()) {
                ingestDiscoveredFiles(userId, connection != null ? connection.getId() : null, discoveredFiles);
            }

            // Diagnostic Pass 1: Duplicate File Detection (SHA-256)
            updateJob(jobId, 45, "in_progress", "Running SHA-256 duplicate detection...", discoveredFiles.size(), failedCount, totalBytes);
            broadcastProgress(jobId, 45, "in_progress", "HASHING_DUPLICATES", storageType,
                    "Evaluating cryptographic SHA-256 hashes...", discoveredFiles.size(),
                    discoveredFiles.size() / 4, 0, 0, 0, 0, 0, failedCount, totalBytes, 0L, null);

            int duplicatesFound = 0;
            if (request.isCheckDuplicates()) {
                duplicateDetector.detectDuplicates(userId);
                duplicatesFound = (int) fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "duplicate");
            }

            if (cancellationFlag.get()) {
                handleCancellation(jobId, startTime, storageType);
                return;
            }

            // Diagnostic Pass 2: Blurry Image Analysis (OpenCV Laplacian)
            updateJob(jobId, 60, "in_progress", "Running Laplacian image sharpness diagnostics...", discoveredFiles.size(), failedCount, totalBytes);
            broadcastProgress(jobId, 60, "in_progress", "AI_IMAGE_ANALYSIS", storageType,
                    "Analyzing image sharpness & clarity with OpenCV...", discoveredFiles.size(),
                    discoveredFiles.size() / 2, duplicatesFound, 0, 0, 0, 0, failedCount, totalBytes, 0L, null);

            int blurryFound = 0;
            if (request.isCheckBlurry()) {
                blurDetector.analyzeImages(userId, request.getBlurThreshold());
                blurryFound = (int) fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "blurry");
            }

            if (cancellationFlag.get()) {
                handleCancellation(jobId, startTime, storageType);
                return;
            }

            // Diagnostic Pass 3: Large File Detection
            updateJob(jobId, 75, "in_progress", "Auditing oversized media and archive assets...", discoveredFiles.size(), failedCount, totalBytes);
            broadcastProgress(jobId, 75, "in_progress", "LARGE_FILE_AUDIT", storageType,
                    "Identifying large storage consumers...", discoveredFiles.size(),
                    (int) (discoveredFiles.size() * 0.75), duplicatesFound, blurryFound, 0, 0, 0, failedCount, totalBytes, 0L, null);

            int largeFound = 0;
            if (request.isCheckLargeFiles()) {
                long customLargeBytes = request.getLargeFileSizeThresholdMB() > 0
                        ? request.getLargeFileSizeThresholdMB() * 1024L * 1024L
                        : 500L * 1024L * 1024L;
                LargeFileDetector.LargeFileThresholds thresholds = new LargeFileDetector.LargeFileThresholds(
                        25L * 1024 * 1024,
                        customLargeBytes,
                        customLargeBytes * 2,
                        100L * 1024 * 1024,
                        customLargeBytes
                );
                largeFileDetector.detectLargeFiles(userId, thresholds);
                largeFound = (int) fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "large");
            }

            if (cancellationFlag.get()) {
                handleCancellation(jobId, startTime, storageType);
                return;
            }

            // Diagnostic Pass 4: Inactive File Audit
            updateJob(jobId, 85, "in_progress", "Evaluating dormancy and timestamp reliability...", discoveredFiles.size(), failedCount, totalBytes);
            int inactiveFound = 0;
            if (request.isCheckInactive()) {
                inactiveFileDetector.detectInactiveFiles(userId, request.getInactiveDaysThreshold());
                inactiveFound = (int) fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "inactive");
            }

            // Diagnostic Pass 5: Temporary File Analysis
            temporaryFileDetector.detectTemporaryFiles(userId);
            int temporaryFound = (int) fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "temporary");

            // Diagnostic Pass 6: Junk Scoring Engine (Composite Evaluation)
            updateJob(jobId, 95, "in_progress", "Synthesizing junk score and risk assessment...", discoveredFiles.size(), failedCount, totalBytes);
            broadcastProgress(jobId, 95, "in_progress", "JUNK_SCORING", storageType,
                    "Synthesizing junk scores and generating recommendations...", discoveredFiles.size(),
                    discoveredFiles.size(), duplicatesFound, blurryFound, largeFound, inactiveFound, temporaryFound,
                    failedCount, totalBytes, 0L, null);
            junkScoringEngine.scoreAllUserFiles(userId);

            if (cancellationFlag.get()) {
                handleCancellation(jobId, startTime, storageType);
                return;
            }

            // Compute actual real-world metrics from MongoDB
            long flaggedCount = fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "duplicate")
                    + fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "blurry")
                    + fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "large")
                    + fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "inactive")
                    + fileItemRepository.countByUserIdAndCategoryAndIsDeletedFalse(userId, "temporary");

            List<FileItem> flaggedItems = fileItemRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                    .filter(f -> !"clean".equalsIgnoreCase(f.getCategory()) && !"Keep Original".equalsIgnoreCase(f.getRecommendationStatus()))
                    .toList();
            long savingsBytes = flaggedItems.stream().mapToLong(FileItem::getSizeBytes).sum();

            ScanJob job = scanJobRepository.findById(jobId).orElse(null);
            if (job != null && !"cancelled".equals(job.getStatus())) {
                job.setProgressPercent(100);
                job.setStatus("completed");
                job.setCompletedTime(Instant.now());
                job.setTotalFilesScanned(discoveredFiles.size());
                job.setFlaggedFilesCount((int) flaggedCount);
                job.setTotalBytesScanned(totalBytes);
                job.setPotentialSavingsBytes(savingsBytes);
                job.setDuration(formatDuration(Duration.between(startTime, Instant.now())));
                job.setActiveDirectory("Scan Completed");
                scanJobRepository.save(job);

                if (connection != null) {
                    connection.setLastScanTime(Instant.now());
                    connectionRepository.save(connection);
                }

                broadcastProgress(jobId, 100, "completed", "COMPLETED", storageType, "Scan Completed",
                        discoveredFiles.size(), discoveredFiles.size(), duplicatesFound, blurryFound, largeFound,
                        inactiveFound, temporaryFound, failedCount, totalBytes, savingsBytes, null);

                notificationService.createNotification(
                        userId,
                        "Storage Scan Completed",
                        String.format("Scan on %s completed in %s. Found %d flagged items.", job.getTargetStorage(), job.getDuration(), job.getFlaggedFilesCount()),
                        "scan_completed",
                        "/files"
                );

                log.info("Intelligent scan job {} completed successfully in {}. Discovered: {}, Flagged: {}",
                        jobId, job.getDuration(), discoveredFiles.size(), flaggedCount);
            }
        } catch (Exception e) {
            log.error("Scan job {} failed with exception", jobId, e);
            ScanJob job = scanJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.setStatus("failed");
                job.setErrorMessage(e.getMessage());
                job.setCompletedTime(Instant.now());
                scanJobRepository.save(job);
            }
            broadcastProgress(jobId, 0, "failed", "FAILED", storageType, "Scan failed: " + e.getMessage(),
                    0, 0, 0, 0, 0, 0, 0, 1, 0L, 0L, e.getMessage());

            notificationService.createNotification(
                    userId,
                    "Storage Scan Failed",
                    "Scan job on target encountered an error: " + e.getMessage(),
                    "scan_failed",
                    "/scanner"
            );
        } finally {
            activeScanCancellations.remove(jobId);
        }
    }

    private void handleCancellation(String jobId, Instant startTime, String storageType) {
        log.warn("Scan job {} was cancelled by user request", jobId);
        ScanJob job = scanJobRepository.findById(jobId).orElse(null);
        if (job != null) {
            job.setStatus("cancelled");
            job.setCompletedTime(Instant.now());
            job.setDuration(formatDuration(Duration.between(startTime, Instant.now())));
            job.setActiveDirectory("Scan cancelled by user");
            scanJobRepository.save(job);
        }
        broadcastProgress(jobId, 0, "cancelled", "CANCELLED", storageType, "Scan cancelled by user",
                0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L, null);
    }

    private void updateJob(String jobId, int progress, String status, String activeDir, int scanned, int failed, long bytes) {
        ScanJob job = scanJobRepository.findById(jobId).orElse(null);
        if (job != null && !"cancelled".equals(job.getStatus())) {
            job.setProgressPercent(progress);
            job.setStatus(status);
            if (activeDir != null) job.setActiveDirectory(activeDir);
            if (scanned > 0) job.setTotalFilesScanned(scanned);
            if (failed > 0) job.setFailedFilesCount(failed);
            if (bytes > 0) job.setTotalBytesScanned(bytes);
            scanJobRepository.save(job);
        }
    }

    private void ingestDiscoveredFiles(String userId, String connectionId, List<ProviderFileMetadata> files) {
        for (ProviderFileMetadata meta : files) {
            if (meta.isDirectory()) continue;

            FileItem item = null;
            if (meta.getProviderFileId() != null && !meta.getProviderFileId().isBlank()) {
                if (connectionId != null) {
                    item = fileItemRepository.findByUserIdAndStorageConnectionIdAndProviderFileIdAndIsDeletedFalse(
                            userId, connectionId, meta.getProviderFileId()).orElse(null);
                }
                if (item == null) {
                    item = fileItemRepository.findByUserIdAndProviderFileIdAndIsDeletedFalse(userId, meta.getProviderFileId()).orElse(null);
                }
            }
            if (item == null && meta.getStorageLocation() != null && !meta.getStorageLocation().isBlank()) {
                item = fileItemRepository.findByUserIdAndStorageLocationAndIsDeletedFalse(userId, meta.getStorageLocation()).orElse(null);
            }
            if (item == null) {
                item = new FileItem();
            }

            item.setUserId(userId);
            item.setStorageConnectionId(connectionId);
            item.setProviderFileId(meta.getProviderFileId());
            item.setName(meta.getName());
            item.setExtension(meta.getExtension());
            item.setMimeType(meta.getMimeType());
            item.setSizeBytes(meta.getSizeBytes());
            item.setStorageLocation(meta.getStorageLocation());
            item.setStorageType(meta.getStorageType());
            item.setStorageProvider(meta.getStorageType() != null ? meta.getStorageType().toUpperCase() : "LOCAL");

            // Extract folder path
            if (meta.getStorageLocation() != null) {
                int lastSlash = Math.max(meta.getStorageLocation().lastIndexOf('/'), meta.getStorageLocation().lastIndexOf('\\'));
                if (lastSlash > 0) {
                    item.setFolderPath(meta.getStorageLocation().substring(0, lastSlash));
                } else {
                    item.setFolderPath(meta.getStorageLocation());
                }
            }

            item.setCreatedTime(meta.getCreatedTime());
            item.setLastModified(meta.getModifiedTime());
            item.setLastAccessed(meta.getLastAccessedTime());
            item.setLastAccessedReliable(meta.isLastAccessedReliable());
            item.setHash(meta.getSha256Hash());
            item.setMd5Hash(meta.getMd5Checksum());
            item.setScanStatus("scanned");
            item.setFileType(deriveFileType(meta.getMimeType(), meta.getExtension()));

            fileItemRepository.save(item);
        }
    }

    private String deriveFileType(String mimeType, String ext) {
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) return "image";
            if (mimeType.startsWith("video/")) return "video";
            if (mimeType.startsWith("audio/")) return "audio";
            if (mimeType.contains("pdf") || mimeType.contains("document") || mimeType.contains("text")) return "document";
            if (mimeType.contains("zip") || mimeType.contains("tar") || mimeType.contains("compressed")) return "archive";
        }
        return "other";
    }

    private StorageConnection resolveStorageConnection(String userId, ScanRequest request) {
        if (request.getStorageConnectionId() != null && !request.getStorageConnectionId().isBlank()) {
            return connectionRepository.findByIdAndUserId(request.getStorageConnectionId(), userId).orElse(null);
        }
        String type = request.getStorageType() != null ? request.getStorageType().toLowerCase() : "local";
        return connectionRepository.findByUserIdAndType(userId, type).orElse(null);
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        long remSec = seconds % 60;
        return minutes + "m " + remSec + "s";
    }

    public PageResponse<ScanJobResponse> getUserScans(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScanJob> jobPage = scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(jobPage.map(ScanJobResponse::new));
    }

    public ScanJobResponse getScanById(String userId, String scanId) {
        ScanJob job = scanJobRepository.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan job not found with id: " + scanId));
        return new ScanJobResponse(job);
    }

    public ScanJobResponse cancelScan(String userId, String scanId) {
        ScanJob job = scanJobRepository.findByIdAndUserId(scanId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Scan job not found with id: " + scanId));

        if ("completed".equalsIgnoreCase(job.getStatus()) || "cancelled".equalsIgnoreCase(job.getStatus())) {
            throw new BadRequestException("Scan job is already finished or cancelled");
        }

        AtomicBoolean flag = activeScanCancellations.get(scanId);
        if (flag != null) {
            flag.set(true);
        }

        job.setStatus("cancelled");
        job.setCompletedTime(Instant.now());
        ScanJob saved = scanJobRepository.save(job);
        broadcastProgress(scanId, 0, "cancelled", "CANCELLED", job.getStorageType(), "Scan cancelled by user",
                0, 0, 0, 0, 0, 0, 0, 0, 0L, 0L, null);
        log.info("Cancelled scan job id: {} for user: {}", scanId, userId);
        return new ScanJobResponse(saved);
    }
}
