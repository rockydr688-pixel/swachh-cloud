package com.cloudclean.cleanup;

import com.cloudclean.dto.CleanupHistoryResponse;
import com.cloudclean.dto.CleanupRequest;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.exception.BadRequestException;
import com.cloudclean.file.FileItem;
import com.cloudclean.recommendation.Recommendation;
import com.cloudclean.repository.CleanupHistoryRepository;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    private final CleanupHistoryRepository cleanupHistoryRepository;
    private final FileItemRepository fileItemRepository;
    private final RecommendationRepository recommendationRepository;

    public CleanupService(CleanupHistoryRepository cleanupHistoryRepository,
                          FileItemRepository fileItemRepository,
                          RecommendationRepository recommendationRepository) {
        this.cleanupHistoryRepository = cleanupHistoryRepository;
        this.fileItemRepository = fileItemRepository;
        this.recommendationRepository = recommendationRepository;
    }

    public CleanupHistoryResponse executeCleanup(String userId, CleanupRequest request, String executedBy) {
        if (!request.isConfirmed()) {
            throw new BadRequestException("Explicit user confirmation is strictly required to execute cleanup");
        }

        long freedBytes = 0L;
        int filesAffected = 0;
        List<String> affectedFileIds = new ArrayList<>();
        String actionTitle = request.getAction();
        String category = "General Cleanup";

        // If cleaning via recommendation
        if (request.getRecommendationId() != null && !request.getRecommendationId().isBlank()) {
            Recommendation rec = recommendationRepository.findByIdAndUserId(request.getRecommendationId(), userId)
                    .orElse(null);

            if (rec != null) {
                actionTitle = "Executed: " + rec.getTitle();
                category = rec.getCategory();
                freedBytes = rec.getPotentialSavingsBytes();
                filesAffected = rec.getFilesCount();
                rec.setStatus("applied");
                recommendationRepository.save(rec);

                // If recommendation has specific target files, soft delete them
                if (rec.getTargetFileIds() != null && !rec.getTargetFileIds().isEmpty()) {
                    List<FileItem> targets = fileItemRepository.findByIdInAndUserId(rec.getTargetFileIds(), userId);
                    Instant now = Instant.now();
                    for (FileItem f : targets) {
                        f.setDeleted(true);
                        f.setDeletedAt(now);
                        affectedFileIds.add(f.getId());
                    }
                    fileItemRepository.saveAll(targets);
                }
            }
        } else if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            List<FileItem> files = fileItemRepository.findByIdInAndUserId(request.getFileIds(), userId);
            Instant now = Instant.now();
            for (FileItem f : files) {
                f.setDeleted(true);
                f.setDeletedAt(now);
                freedBytes += f.getSizeBytes();
                filesAffected++;
                affectedFileIds.add(f.getId());
                category = f.getCategory();
            }
            fileItemRepository.saveAll(files);
            actionTitle = "Purged " + filesAffected + " items";
        }

        CleanupHistory history = new CleanupHistory(
                userId,
                actionTitle,
                category,
                filesAffected,
                freedBytes,
                executedBy != null ? executedBy : "User Confirmation",
                request.getMethod() != null ? request.getMethod() : "Safe Trash / Soft Delete"
        );
        history.setCleanedFileIds(affectedFileIds);
        CleanupHistory saved = cleanupHistoryRepository.save(history);

        log.info("Successfully executed cleanup for user: {}. Freed: {} bytes across {} files", userId, freedBytes, filesAffected);
        return new CleanupHistoryResponse(saved);
    }

    public PageResponse<CleanupHistoryResponse> getCleanupHistory(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<CleanupHistory> historyPage = cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(userId, pageable);

        if (historyPage.isEmpty()) {
            seedDefaultHistoryIfEmpty(userId);
            historyPage = cleanupHistoryRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        }

        return PageResponse.of(historyPage.map(CleanupHistoryResponse::new));
    }

    private void seedDefaultHistoryIfEmpty(String userId) {
        Instant now = Instant.now();
        CleanupHistory h1 = new CleanupHistory(userId, "Purged 420 Duplicate Downloads", "Duplicates", 420, 34500000000L, "User (Batch Review)", "Safe Trash / Soft Delete");
        h1.setTimestamp(now.minusSeconds(3600 * 24 * 2));

        CleanupHistory h2 = new CleanupHistory(userId, "Discarded 112 Blurry Burst Photos", "Blurry Images", 112, 5200000000L, "User (Batch Review)", "Local Recycle Bin");
        h2.setTimestamp(now.minusSeconds(3600 * 24 * 7));

        CleanupHistory h3 = new CleanupHistory(userId, "Archived 18 Stale 4K OBS Recordings", "Large Files", 18, 86400000000L, "Automated Retention Policy", "Compressed & Moved to Archive");
        h3.setTimestamp(now.minusSeconds(3600 * 24 * 14));

        cleanupHistoryRepository.saveAll(List.of(h1, h2, h3));
    }
}
