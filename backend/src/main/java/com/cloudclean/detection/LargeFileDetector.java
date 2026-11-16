package com.cloudclean.detection;

import com.cloudclean.analysis.FileAnalysis;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileAnalysisRepository;
import com.cloudclean.repository.FileItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LargeFileDetector {

    private static final Logger log = LoggerFactory.getLogger(LargeFileDetector.class);

    // Default categorized thresholds in bytes
    public static final long DEFAULT_IMAGE_THRESHOLD = 25L * 1024 * 1024;       // 25 MB
    public static final long DEFAULT_VIDEO_THRESHOLD = 500L * 1024 * 1024;      // 500 MB
    public static final long DEFAULT_ARCHIVE_THRESHOLD = 1024L * 1024 * 1024;   // 1 GB
    public static final long DEFAULT_DOCUMENT_THRESHOLD = 100L * 1024 * 1024;   // 100 MB
    public static final long DEFAULT_OTHER_THRESHOLD = 500L * 1024 * 1024;      // 500 MB

    private final FileItemRepository fileItemRepository;
    private final FileAnalysisRepository fileAnalysisRepository;

    public LargeFileDetector(FileItemRepository fileItemRepository,
                             FileAnalysisRepository fileAnalysisRepository) {
        this.fileItemRepository = fileItemRepository;
        this.fileAnalysisRepository = fileAnalysisRepository;
    }

    public record LargeFileThresholds(
            Long imageBytes,
            Long videoBytes,
            Long archiveBytes,
            Long documentBytes,
            Long otherBytes
    ) {
        public static LargeFileThresholds defaults() {
            return new LargeFileThresholds(
                    DEFAULT_IMAGE_THRESHOLD,
                    DEFAULT_VIDEO_THRESHOLD,
                    DEFAULT_ARCHIVE_THRESHOLD,
                    DEFAULT_DOCUMENT_THRESHOLD,
                    DEFAULT_OTHER_THRESHOLD
            );
        }
    }

    public record LargeFileReport(
            int totalEvaluated,
            int flaggedLargeCount,
            long totalLargeBytes,
            Map<String, Integer> categoryBreakdown
    ) {}

    public LargeFileReport detectLargeFiles(String userId, LargeFileThresholds customThresholds) {
        LargeFileThresholds thresholds = customThresholds != null ? customThresholds : LargeFileThresholds.defaults();
        List<FileItem> allFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);

        List<FileAnalysis> analyses = new ArrayList<>();
        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("image", 0);
        breakdown.put("video", 0);
        breakdown.put("archive", 0);
        breakdown.put("document", 0);
        breakdown.put("other", 0);

        int flaggedCount = 0;
        long largeBytes = 0L;

        for (FileItem file : allFiles) {
            String type = file.getFileType() != null ? file.getFileType().toLowerCase() : "other";
            long threshold = getThresholdForType(type, thresholds);

            boolean isLarge = file.getSizeBytes() >= threshold;

            if (isLarge) {
                flaggedCount++;
                largeBytes += file.getSizeBytes();
                breakdown.put(type, breakdown.getOrDefault(type, 0) + 1);

                if ("clean".equals(file.getCategory())) {
                    file.setCategory("large");
                    file.setRecommendationStatus("Review & Delete");
                    double sizeMB = file.getSizeBytes() / (1024.0 * 1024.0);
                    double threshMB = threshold / (1024.0 * 1024.0);
                    file.setDetectionReason(String.format("Oversized %s asset: %.1f MB (exceeds %.0f MB threshold)", type, sizeMB, threshMB));
                }
            }

            FileAnalysis analysis = new FileAnalysis(
                    userId,
                    file.getId(),
                    "large",
                    (double) file.getSizeBytes() / Math.max(threshold, 1L),
                    isLarge,
                    file.getDetectionReason()
            );

            analysis.setRiskCategory("low");
            analysis.setRecommendedAction(isLarge ? "Review & Delete" : "None");

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("file_type", type);
            metrics.put("size_bytes", file.getSizeBytes());
            metrics.put("threshold_bytes", threshold);
            metrics.put("is_large", isLarge);

            analysis.setMetrics(metrics);
            analyses.add(analysis);
        }

        fileItemRepository.saveAll(allFiles);
        fileAnalysisRepository.saveAll(analyses);

        log.info("Large file detection completed for user {}: {} files analyzed, {} flagged, {} total bytes",
                userId, allFiles.size(), flaggedCount, largeBytes);

        return new LargeFileReport(allFiles.size(), flaggedCount, largeBytes, breakdown);
    }

    public long getThresholdForType(String fileType, LargeFileThresholds thresholds) {
        return switch (fileType.toLowerCase()) {
            case "image" -> thresholds.imageBytes() != null ? thresholds.imageBytes() : DEFAULT_IMAGE_THRESHOLD;
            case "video" -> thresholds.videoBytes() != null ? thresholds.videoBytes() : DEFAULT_VIDEO_THRESHOLD;
            case "archive" -> thresholds.archiveBytes() != null ? thresholds.archiveBytes() : DEFAULT_ARCHIVE_THRESHOLD;
            case "document" -> thresholds.documentBytes() != null ? thresholds.documentBytes() : DEFAULT_DOCUMENT_THRESHOLD;
            default -> thresholds.otherBytes() != null ? thresholds.otherBytes() : DEFAULT_OTHER_THRESHOLD;
        };
    }
}
