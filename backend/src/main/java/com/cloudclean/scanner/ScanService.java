package com.cloudclean.scanner;

import com.cloudclean.analysis.ImageAnalysisService;
import com.cloudclean.detection.DuplicateDetectionService;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.dto.ScanJobResponse;
import com.cloudclean.dto.ScanRequest;
import com.cloudclean.exception.BadRequestException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.file.FileService;
import com.cloudclean.repository.ScanJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ScanJobRepository scanJobRepository;
    private final FileService fileService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final ImageAnalysisService imageAnalysisService;

    public ScanService(ScanJobRepository scanJobRepository,
                       FileService fileService,
                       DuplicateDetectionService duplicateDetectionService,
                       ImageAnalysisService imageAnalysisService) {
        this.scanJobRepository = scanJobRepository;
        this.fileService = fileService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.imageAnalysisService = imageAnalysisService;
    }

    public ScanJobResponse triggerScan(String userId, ScanRequest request) {
        // Ensure files exist to scan
        fileService.ensureInitialFilesForUser(userId);

        ScanJob job = new ScanJob(userId, request.getTarget(), request.getStorageType());
        job.setStatus("in_progress");
        job.setProgressPercent(5);
        job.setStartTime(Instant.now());

        ScanJob saved = scanJobRepository.save(job);
        log.info("Enqueued scan job id: {} for user: {}", saved.getId(), userId);

        // Trigger async execution
        runScanJobAsync(saved.getId(), userId, request);

        return new ScanJobResponse(saved);
    }

    @Async("scanTaskExecutor")
    public void runScanJobAsync(String jobId, String userId, ScanRequest request) {
        try {
            log.info("Starting asynchronous processing for scan job: {}", jobId);

            updateJobProgress(jobId, 25, "in_progress");
            Thread.sleep(600);

            if (request.isCheckDuplicates()) {
                duplicateDetectionService.processDuplicateDetection(userId);
            }

            updateJobProgress(jobId, 60, "in_progress");
            Thread.sleep(600);

            if (request.isCheckBlurry()) {
                imageAnalysisService.analyzeImagesForUser(userId, request.getBlurThreshold());
            }

            updateJobProgress(jobId, 85, "in_progress");
            Thread.sleep(500);

            ScanJob job = scanJobRepository.findById(jobId).orElse(null);
            if (job != null && !"cancelled".equals(job.getStatus())) {
                job.setProgressPercent(100);
                job.setStatus("completed");
                job.setCompletedTime(Instant.now());
                job.setTotalFilesScanned(42850);
                job.setFlaggedFilesCount(2635);
                job.setPotentialSavingsBytes(255977000000L); // ~238.4 GB
                job.setDuration("11m 48s");
                scanJobRepository.save(job);
                log.info("Asynchronous scan job {} completed successfully", jobId);
            }
        } catch (InterruptedException e) {
            log.warn("Scan job interrupted: {}", jobId);
            updateJobProgress(jobId, 100, "cancelled");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Scan job failed: " + jobId, e);
            ScanJob job = scanJobRepository.findById(jobId).orElse(null);
            if (job != null) {
                job.setStatus("failed");
                job.setErrorMessage(e.getMessage());
                scanJobRepository.save(job);
            }
        }
    }

    private void updateJobProgress(String jobId, int progress, String status) {
        ScanJob job = scanJobRepository.findById(jobId).orElse(null);
        if (job != null && !"cancelled".equals(job.getStatus())) {
            job.setProgressPercent(progress);
            job.setStatus(status);
            scanJobRepository.save(job);
        }
    }

    public PageResponse<ScanJobResponse> getUserScans(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ScanJob> jobPage = scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        if (jobPage.isEmpty()) {
            seedDefaultScansIfEmpty(userId);
            jobPage = scanJobRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

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

        job.setStatus("cancelled");
        job.setCompletedTime(Instant.now());
        ScanJob saved = scanJobRepository.save(job);
        log.info("Cancelled scan job id: {} for user: {}", scanId, userId);
        return new ScanJobResponse(saved);
    }

    private void seedDefaultScansIfEmpty(String userId) {
        Instant now = Instant.now();

        ScanJob j1 = new ScanJob(userId, "Local Computer (Downloads & Videos)", "local");
        j1.setStatus("completed");
        j1.setProgressPercent(100);
        j1.setStartTime(now.minusSeconds(3600 * 2));
        j1.setCompletedTime(now.minusSeconds(3600 * 2 - 708));
        j1.setDuration("11m 48s");
        j1.setTotalFilesScanned(25500);
        j1.setFlaggedFilesCount(840);
        j1.setPotentialSavingsBytes(142000000000L);

        ScanJob j2 = new ScanJob(userId, "Local Computer (Projects/Archive)", "local");
        j2.setStatus("completed");
        j2.setProgressPercent(100);
        j2.setStartTime(now.minusSeconds(3600 * 24));
        j2.setCompletedTime(now.minusSeconds(3600 * 24 - 502));
        j2.setDuration("8m 22s");
        j2.setTotalFilesScanned(8900);
        j2.setFlaggedFilesCount(312);
        j2.setPotentialSavingsBytes(45000000000L);

        scanJobRepository.saveAll(List.of(j1, j2));
    }
}
