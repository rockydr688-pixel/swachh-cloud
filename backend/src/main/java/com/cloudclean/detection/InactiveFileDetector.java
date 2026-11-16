package com.cloudclean.detection;

import com.cloudclean.analysis.FileAnalysis;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class InactiveFileDetector {

    private static final Logger log = LoggerFactory.getLogger(InactiveFileDetector.class);

    public static final int THRESHOLD_30_DAYS = 30;
    public static final int THRESHOLD_90_DAYS = 90;
    public static final int THRESHOLD_180_DAYS = 180;
    public static final int THRESHOLD_365_DAYS = 365;

    private final FileItemRepository fileItemRepository;
    private final FileAnalysisRepository fileAnalysisRepository;

    public InactiveFileDetector(FileItemRepository fileItemRepository,
                                FileAnalysisRepository fileAnalysisRepository) {
        this.fileItemRepository = fileItemRepository;
        this.fileAnalysisRepository = fileAnalysisRepository;
    }

    public record InactivityReport(
            int totalEvaluated,
            int flaggedInactiveCount,
            long potentialInactiveBytes,
            Map<String, Integer> categoryDistribution
    ) {}

    public InactivityReport detectInactiveFiles(String userId, int customDaysThreshold) {
        int threshold = customDaysThreshold > 0 ? customDaysThreshold : THRESHOLD_180_DAYS;
        Instant now = Instant.now();

        List<FileItem> allFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);
        List<FileAnalysis> analyses = new ArrayList<>();

        Map<String, Integer> categoryCount = new HashMap<>();
        categoryCount.put("30_days", 0);
        categoryCount.put("90_days", 0);
        categoryCount.put("180_days", 0);
        categoryCount.put("365_days", 0);
        categoryCount.put("active", 0);

        int flaggedCount = 0;
        long inactiveBytes = 0L;

        for (FileItem file : allFiles) {
            boolean accessReliable = file.isLastAccessedReliable() && file.getLastAccessed() != null;
            Instant referenceTime;
            String signalUsed;

            if (accessReliable) {
                referenceTime = file.getLastAccessed();
                signalUsed = "last_accessed";
            } else {
                referenceTime = file.getLastModified() != null ? file.getLastModified() : file.getCreatedTime();
                signalUsed = "last_modified";
            }

            if (referenceTime == null) {
                file.setInactivityCategory("active");
                categoryCount.put("active", categoryCount.get("active") + 1);
                continue;
            }

            long daysOld = Math.max(0, Duration.between(referenceTime, now).toDays());
            String inactivityBucket = determineBucket(daysOld);
            file.setInactivityCategory(inactivityBucket);
            categoryCount.put(inactivityBucket, categoryCount.getOrDefault(inactivityBucket, 0) + 1);

            boolean isFlagged = daysOld >= threshold;

            if (isFlagged && !"duplicate".equals(file.getCategory()) && !"blurry".equals(file.getCategory())) {
                flaggedCount++;
                inactiveBytes += file.getSizeBytes();
                file.setCategory("inactive");

                if (daysOld >= THRESHOLD_365_DAYS) {
                    file.setRecommendationStatus("Safe to Delete");
                } else {
                    file.setRecommendationStatus("Recommended for Archival");
                }

                if (accessReliable) {
                    file.setDetectionReason(String.format("Dormant file: unaccessed for %d days (%s bucket)", daysOld, inactivityBucket));
                } else {
                    file.setDetectionReason(String.format("Dormant file: %d days since modification (%s bucket, host access timestamp unavailable)", daysOld, inactivityBucket));
                }
            }

            FileAnalysis analysis = new FileAnalysis(
                    userId,
                    file.getId(),
                    "inactivity",
                    daysOld,
                    isFlagged,
                    file.getDetectionReason()
            );

            analysis.setRiskCategory(daysOld >= THRESHOLD_365_DAYS ? "low" : "medium");
            analysis.setRecommendedAction(file.getRecommendationStatus());

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("days_inactive", daysOld);
            metrics.put("inactivity_category", inactivityBucket);
            metrics.put("signal_used", signalUsed);
            metrics.put("access_timestamp_reliable", accessReliable);
            metrics.put("threshold_days", threshold);
            metrics.put("reference_timestamp", referenceTime);

            analysis.setMetrics(metrics);
            analyses.add(analysis);
        }

        fileItemRepository.saveAll(allFiles);
        fileAnalysisRepository.saveAll(analyses);

        log.info("Inactivity evaluation completed for user {}: {} files analyzed, {} flagged, {} bytes",
                userId, allFiles.size(), flaggedCount, inactiveBytes);

        return new InactivityReport(allFiles.size(), flaggedCount, inactiveBytes, categoryCount);
    }

    public static String determineBucket(long daysOld) {
        if (daysOld >= THRESHOLD_365_DAYS) return "365_days";
        if (daysOld >= THRESHOLD_180_DAYS) return "180_days";
        if (daysOld >= THRESHOLD_90_DAYS) return "90_days";
        if (daysOld >= THRESHOLD_30_DAYS) return "30_days";
        return "active";
    }
}
