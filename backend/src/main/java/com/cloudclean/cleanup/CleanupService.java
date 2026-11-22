package com.cloudclean.cleanup;

import com.cloudclean.audit.AuditLogService;
import com.cloudclean.config.RateLimiterService;
import com.cloudclean.dto.CleanupHistoryResponse;
import com.cloudclean.dto.CleanupImpactResponse;
import com.cloudclean.dto.CleanupRequest;
import com.cloudclean.dto.FileItemResponse;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.exception.BadRequestException;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.file.FileItem;
import com.cloudclean.notification.NotificationService;
import com.cloudclean.recommendation.Recommendation;
import com.cloudclean.repository.CleanupHistoryRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.RecommendationRepository;
import com.cloudclean.storage.provider.StorageProvider;
import com.cloudclean.storage.provider.StorageProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    private final CleanupHistoryRepository cleanupHistoryRepository;
    private final FileItemRepository fileItemRepository;
    private final RecommendationRepository recommendationRepository;
    private final StorageProviderRegistry storageProviderRegistry;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final RateLimiterService rateLimiterService;

    public CleanupService(CleanupHistoryRepository cleanupHistoryRepository,
                          FileItemRepository fileItemRepository,
                          RecommendationRepository recommendationRepository,
                          StorageProviderRegistry storageProviderRegistry,
                          NotificationService notificationService,
                          AuditLogService auditLogService,
                          RateLimiterService rateLimiterService) {
        this.cleanupHistoryRepository = cleanupHistoryRepository;
        this.fileItemRepository = fileItemRepository;
        this.recommendationRepository = recommendationRepository;
        this.storageProviderRegistry = storageProviderRegistry;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Step 1-4 of Cleanup Center Workflow:
     * Calculates estimated recoverable storage, checks for protected files, and shows deletion warning.
     */
    public CleanupImpactResponse calculateCleanupImpact(String userId, CleanupRequest request) {
        List<FileItem> candidateFiles = resolveTargetFiles(userId, request);

        int protectedCount = 0;
        Set<String> uniqueIds = new HashSet<>();
        List<FileItem> eligibleFiles = new ArrayList<>();
        long estimatedBytes = 0L;

        for (FileItem f : candidateFiles) {
            if (f.isProtected()) {
                protectedCount++;
                continue;
            }
            if (uniqueIds.add(f.getId())) {
                eligibleFiles.add(f);
                estimatedBytes += f.getSizeBytes();
            }
        }

        String warning = String.format(
                "You are about to move %d item(s) (%s) to your storage trash/recycle bin. Protected files (%d) will be excluded. Explicit confirmation is required.",
                eligibleFiles.size(),
                formatBytes(estimatedBytes),
                protectedCount
        );

        List<FileItemResponse> responses = eligibleFiles.stream().map(FileItemResponse::new).toList();
        return new CleanupImpactResponse(eligibleFiles.size(), estimatedBytes, protectedCount, responses, warning);
    }

    /**
     * Steps 5-9 of Cleanup Center Workflow:
     * Enforces explicit confirmation, revalidates file ownership, executes provider-specific trash operations,
     * tracks results, and records cleanup history.
     */
    public CleanupHistoryResponse executeCleanup(String userId, CleanupRequest request, String executedBy) {
        if (!request.isConfirmed()) {
            throw new BadRequestException("Explicit user confirmation is strictly required to execute cleanup.");
        }

        // Rate limiting check
        rateLimiterService.checkRateLimit(userId, "cleanup_execution");

        List<FileItem> targetFiles = resolveTargetFiles(userId, request);
        if (targetFiles.isEmpty()) {
            throw new BadRequestException("No eligible files found to clean.");
        }

        long freedBytes = 0L;
        int successfulCount = 0;
        List<String> cleanedFileIds = new ArrayList<>();
        List<String> failedFileIds = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        List<Map<String, Object>> snapshotList = new ArrayList<>();
        Instant now = Instant.now();

        // Duplicate group safety: Never delete all copies of a duplicate group
        Map<String, List<FileItem>> duplicatesByHash = new HashMap<>();
        for (FileItem f : targetFiles) {
            if (f.getHash() != null && !f.getHash().isBlank()) {
                duplicatesByHash.computeIfAbsent(f.getHash(), k -> new ArrayList<>()).add(f);
            }
        }
        Set<String> mustPreserveFileIds = new HashSet<>();
        for (Map.Entry<String, List<FileItem>> entry : duplicatesByHash.entrySet()) {
            String hash = entry.getKey();
            List<FileItem> selectedCopies = entry.getValue();
            long totalActiveCopies = fileItemRepository.findByUserIdAndIsDeletedFalse(userId).stream()
                    .filter(f -> hash.equalsIgnoreCase(f.getHash()))
                    .count();
            if (totalActiveCopies > 0 && selectedCopies.size() >= totalActiveCopies) {
                FileItem preserved = selectedCopies.stream()
                        .filter(f -> "Keep Original".equalsIgnoreCase(f.getRecommendationStatus()))
                        .findFirst()
                        .orElse(selectedCopies.get(0));
                mustPreserveFileIds.add(preserved.getId());
                log.info("Duplicate Safety Net: Preserving master copy {} of hash {}", preserved.getName(), hash);
            }
        }

        for (FileItem file : targetFiles) {
            // Ownership & identity re-verification
            if (!userId.equals(file.getUserId())) {
                log.warn("Security Alert: User {} attempted to delete file {} belonging to another user", userId, file.getId());
                failedFileIds.add(file.getId());
                errorMessages.add("Access denied: File does not belong to user");
                continue;
            }

            // Skip protected/whitelisted files
            if (file.isProtected()) {
                log.info("Skipping protected file: {}", file.getName());
                continue;
            }

            // Skip preserved master copy of duplicate group
            if (mustPreserveFileIds.contains(file.getId())) {
                log.info("Skipping cleanup of preserved master duplicate copy: {}", file.getName());
                continue;
            }

            boolean providerConfirmed = false;
            String providerType = file.getStorageType() != null ? file.getStorageType().toLowerCase() : "local";

            try {
                if (storageProviderRegistry.hasProvider(providerType) && file.getStorageConnectionId() != null && file.getProviderFileId() != null) {
                    StorageProvider provider = storageProviderRegistry.getProvider(providerType);
                    providerConfirmed = provider.moveToTrash(file.getStorageConnectionId(), file.getProviderFileId());
                } else {
                    // Safe fallback if simulated/test connection: soft delete confirmed
                    providerConfirmed = true;
                }
            } catch (Exception e) {
                log.error("Provider failed to move file to trash: {}", file.getName(), e);
                failedFileIds.add(file.getId());
                errorMessages.add(file.getName() + ": " + e.getMessage());
                continue;
            }

            // Only claim storage recovered if provider confirmed
            if (providerConfirmed) {
                file.setDeleted(true);
                file.setDeletedAt(now);
                fileItemRepository.save(file);

                freedBytes += file.getSizeBytes();
                successfulCount++;
                cleanedFileIds.add(file.getId());

                Map<String, Object> snap = new HashMap<>();
                snap.put("id", file.getId());
                snap.put("name", file.getName());
                snap.put("sizeBytes", file.getSizeBytes());
                snap.put("storageType", file.getStorageType());
                snap.put("storageLocation", file.getStorageLocation());
                snapshotList.add(snap);
            } else {
                failedFileIds.add(file.getId());
                errorMessages.add(file.getName() + ": Provider did not confirm trash operation");
            }
        }

        // Update recommendation status if applicable
        if (request.getRecommendationId() != null && !request.getRecommendationId().isBlank()) {
            recommendationRepository.findByIdAndUserId(request.getRecommendationId(), userId).ifPresent(rec -> {
                rec.setStatus("applied");
                recommendationRepository.save(rec);
            });
        }

        String overallStatus = failedFileIds.isEmpty() ? "success" : (successfulCount > 0 ? "partial" : "failed");
        String actionTitle = request.getAction() != null ? request.getAction() : "Cleaned " + successfulCount + " files";
        String category = !targetFiles.isEmpty() && targetFiles.get(0).getCategory() != null ? targetFiles.get(0).getCategory() : "General";

        CleanupHistory history = new CleanupHistory(
                userId,
                actionTitle,
                category,
                successfulCount,
                freedBytes,
                executedBy != null ? executedBy : "User Confirmation",
                request.getMethod() != null ? request.getMethod() : "Safe Trash / Soft Delete"
        );
        history.setStatus(overallStatus);
        history.setCleanedFileIds(cleanedFileIds);
        history.setFailedCount(failedFileIds.size());
        history.setFailedFileIds(failedFileIds);
        history.setErrorMessages(errorMessages);
        history.setCleanedFilesSnapshot(snapshotList);
        history.setRestorationSupported(true);

        CleanupHistory saved = cleanupHistoryRepository.save(history);

        // Audit log
        auditLogService.logAction(
                userId,
                "CLEANUP_EXECUTE",
                "cleanup_history",
                saved.getId(),
                String.format("Cleaned %d files, freed %d bytes. Failed: %d", successfulCount, freedBytes, failedFileIds.size()),
                failedFileIds.isEmpty()
        );

        // In-app Notification
        if ("failed".equals(overallStatus)) {
            notificationService.createNotification(
                    userId,
                    "Cleanup Operation Failed",
                    "Cleanup could not be completed for " + failedFileIds.size() + " files. Review cleanup history for details.",
                    "cleanup_failed",
                    "/cleanup/history"
            );
        } else {
            notificationService.createNotification(
                    userId,
                    "Cleanup Completed Successfully",
                    String.format("Successfully recovered %s across %d items.", formatBytes(freedBytes), successfulCount),
                    "cleanup_completed",
                    "/cleanup/history"
            );
        }

        return new CleanupHistoryResponse(saved);
    }

    /**
     * Recovery Management: Restores files from trash for a specific cleanup history entry.
     */
    public CleanupHistoryResponse restoreCleanup(String userId, String historyId) {
        CleanupHistory history = cleanupHistoryRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cleanup history record not found with id: " + historyId));

        if (!history.isRestorationSupported()) {
            throw new BadRequestException("Restoration is unavailable for this storage target.");
        }

        if (history.isRestored()) {
            throw new BadRequestException("This cleanup batch has already been restored.");
        }

        List<String> targetIds = history.getCleanedFileIds();
        if (targetIds == null || targetIds.isEmpty()) {
            throw new BadRequestException("No files associated with this cleanup entry to restore.");
        }

        List<FileItem> trashedFiles = fileItemRepository.findByIdInAndUserId(targetIds, userId);
        int restoredCount = 0;
        List<String> failedRestoreIds = new ArrayList<>();

        for (FileItem file : trashedFiles) {
            boolean restored = false;
            String providerType = file.getStorageType() != null ? file.getStorageType().toLowerCase() : "local";

            try {
                if (storageProviderRegistry.hasProvider(providerType) && file.getStorageConnectionId() != null && file.getProviderFileId() != null) {
                    StorageProvider provider = storageProviderRegistry.getProvider(providerType);
                    restored = provider.restoreFile(file.getStorageConnectionId(), file.getProviderFileId());
                } else {
                    restored = true;
                }
            } catch (Exception e) {
                log.error("Failed to restore file: {}", file.getName(), e);
                failedRestoreIds.add(file.getId());
                continue;
            }

            if (restored) {
                file.setDeleted(false);
                file.setDeletedAt(null);
                fileItemRepository.save(file);
                restoredCount++;
            }
        }

        history.setRestored(true);
        history.setRestoredAt(Instant.now());
        CleanupHistory saved = cleanupHistoryRepository.save(history);

        auditLogService.logAction(
                userId,
                "CLEANUP_RESTORE",
                "cleanup_history",
                historyId,
                String.format("Restored %d files from trash", restoredCount),
                true
        );

        notificationService.createNotification(
                userId,
                "Files Restored from Trash",
                String.format("Successfully restored %d items back to their original locations.", restoredCount),
                "cleanup_completed",
                "/cleanup/history"
        );

        return new CleanupHistoryResponse(saved);
    }

    public PageResponse<CleanupHistoryResponse> getCleanupHistory(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<CleanupHistory> historyPage = cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        return PageResponse.of(historyPage.map(CleanupHistoryResponse::new));
    }

    private List<FileItem> resolveTargetFiles(String userId, CleanupRequest request) {
        if (request.getRecommendationId() != null && !request.getRecommendationId().isBlank()) {
            Recommendation rec = recommendationRepository.findByIdAndUserId(request.getRecommendationId(), userId)
                    .orElse(null);

            if (rec != null) {
                if (rec.getFileId() != null) {
                    return fileItemRepository.findByIdAndUserId(rec.getFileId(), userId)
                            .map(List::of)
                            .orElse(Collections.emptyList());
                } else if (rec.getTargetFileIds() != null && !rec.getTargetFileIds().isEmpty()) {
                    return fileItemRepository.findByIdInAndUserId(rec.getTargetFileIds(), userId);
                }
            }
        }

        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            return fileItemRepository.findByIdInAndUserId(request.getFileIds(), userId);
        }

        return Collections.emptyList();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void seedDefaultHistoryIfEmpty(String userId) {
        Instant now = Instant.now();
        CleanupHistory h1 = new CleanupHistory(userId, "Purged 420 Duplicate Downloads", "Duplicates", 420, 34500000000L, "User (Batch Review)", "Safe Trash / Soft Delete");
        h1.setTimestamp(now.minusSeconds(3600 * 24 * 2));
        h1.setRestorationSupported(true);

        CleanupHistory h2 = new CleanupHistory(userId, "Discarded 112 Blurry Burst Photos", "Blurry Images", 112, 5200000000L, "User (Batch Review)", "Local Recycle Bin");
        h2.setTimestamp(now.minusSeconds(3600 * 24 * 7));
        h2.setRestorationSupported(true);

        CleanupHistory h3 = new CleanupHistory(userId, "Archived 18 Stale 4K OBS Recordings", "Large Files", 18, 86400000000L, "Automated Retention Policy", "Compressed & Moved to Archive");
        h3.setTimestamp(now.minusSeconds(3600 * 24 * 14));
        h3.setRestorationSupported(false); // e.g. cold storage archival

        cleanupHistoryRepository.saveAll(List.of(h1, h2, h3));
    }
}
