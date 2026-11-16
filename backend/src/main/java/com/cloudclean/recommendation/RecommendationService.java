package com.cloudclean.recommendation;

import com.cloudclean.audit.AuditLogService;
import com.cloudclean.dto.RecommendationResponse;
import com.cloudclean.dto.RecommendationUpdateRequest;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.file.FileItem;
import com.cloudclean.notification.NotificationService;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final RecommendationRepository recommendationRepository;
    private final FileItemRepository fileItemRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                 FileItemRepository fileItemRepository,
                                 NotificationService notificationService,
                                 AuditLogService auditLogService) {
        this.recommendationRepository = recommendationRepository;
        this.fileItemRepository = fileItemRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    public List<RecommendationResponse> getUserRecommendations(String userId) {
        return getUserRecommendations(userId, null, null, null, null);
    }

    public List<RecommendationResponse> getUserRecommendations(String userId, String category, String riskLevel, String status, Integer minScore) {
        List<Recommendation> recommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (recommendations.isEmpty()) {
            recommendations = generateRecommendationsFromDetections(userId);
        }

        return recommendations.stream()
                .filter(r -> category == null || category.equalsIgnoreCase("all") || category.equalsIgnoreCase(r.getCategory()) || category.equalsIgnoreCase(r.getType()))
                .filter(r -> riskLevel == null || riskLevel.equalsIgnoreCase("all") || riskLevel.equalsIgnoreCase(r.getRiskLevel()))
                .filter(r -> status == null || status.equalsIgnoreCase("all") || status.equalsIgnoreCase(r.getStatus()))
                .filter(r -> minScore == null || r.getJunkScore() >= minScore)
                .map(RecommendationResponse::new)
                .collect(Collectors.toList());
    }

    public RecommendationResponse getRecommendationById(String userId, String id) {
        Recommendation rec = recommendationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + id));
        return new RecommendationResponse(rec);
    }

    public RecommendationResponse updateRecommendationStatus(String userId, String id, RecommendationUpdateRequest request) {
        Recommendation rec = recommendationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + id));

        rec.setStatus(request.getStatus().toLowerCase());
        Recommendation saved = recommendationRepository.save(rec);
        auditLogService.logAction(userId, "UPDATE_RECOMMENDATION_STATUS", "recommendation", id, "Status updated to " + request.getStatus(), true);
        return new RecommendationResponse(saved);
    }

    public RecommendationResponse acceptRecommendation(String userId, String id) {
        Recommendation rec = recommendationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + id));
        rec.setStatus("accepted");
        Recommendation saved = recommendationRepository.save(rec);
        auditLogService.logAction(userId, "ACCEPT_RECOMMENDATION", "recommendation", id, "Accepted recommendation: " + rec.getTitle(), true);
        return new RecommendationResponse(saved);
    }

    public RecommendationResponse rejectRecommendation(String userId, String id) {
        Recommendation rec = recommendationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found with id: " + id));
        rec.setStatus("rejected");
        Recommendation saved = recommendationRepository.save(rec);
        auditLogService.logAction(userId, "REJECT_RECOMMENDATION", "recommendation", id, "Rejected recommendation: " + rec.getTitle(), true);
        return new RecommendationResponse(saved);
    }

    public RecommendationResponse excludeFile(String userId, String idOrFileId) {
        // Check if ID is a recommendation ID or a file ID
        Optional<Recommendation> recOpt = recommendationRepository.findByIdAndUserId(idOrFileId, userId);
        Recommendation rec;
        String fileId;

        if (recOpt.isPresent()) {
            rec = recOpt.get();
            rec.setStatus("excluded");
            rec.setExcluded(true);
            fileId = rec.getFileId();
            recommendationRepository.save(rec);
        } else {
            fileId = idOrFileId;
        }

        if (fileId != null) {
            fileItemRepository.findByIdAndUserId(fileId, userId).ifPresent(file -> {
                file.setExcluded(true);
                fileItemRepository.save(file);
            });
        }

        auditLogService.logAction(userId, "EXCLUDE_FILE", "file", fileId, "Excluded file from cleanup recommendations", true);
        return recOpt.map(RecommendationResponse::new).orElse(null);
    }

    public FileItem protectFile(String userId, String fileId) {
        FileItem file = fileItemRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        file.setProtected(true);
        file.setJunkScore(0);
        file.setRecommendationStatus("Keep Original");
        file.setRiskCategory("high");
        FileItem saved = fileItemRepository.save(file);

        // Mark any matching recommendation as protected
        recommendationRepository.findByUserIdAndFileId(userId, fileId).ifPresent(r -> {
            r.setStatus("protected");
            r.setProtected(true);
            r.setRecommendedAction("Keep Original");
            recommendationRepository.save(r);
        });

        auditLogService.logAction(userId, "PROTECT_FILE", "file", fileId, "Whitelisted and protected file: " + file.getName(), true);
        return saved;
    }

    public FileItem unprotectFile(String userId, String fileId) {
        FileItem file = fileItemRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        file.setProtected(false);
        FileItem saved = fileItemRepository.save(file);

        recommendationRepository.findByUserIdAndFileId(userId, fileId).ifPresent(r -> {
            r.setStatus("pending");
            r.setProtected(false);
            recommendationRepository.save(r);
        });

        auditLogService.logAction(userId, "UNPROTECT_FILE", "file", fileId, "Removed protection for file: " + file.getName(), true);
        return saved;
    }

    /**
     * Generates real recommendations based on actual scan/detection results from MongoDB.
     */
    public List<Recommendation> generateRecommendationsFromDetections(String userId) {
        List<FileItem> activeFiles = fileItemRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Recommendation> generated = new ArrayList<>();

        if (activeFiles.isEmpty()) {
            return generateDefaultRecommendations(userId);
        }

        // 1. Generate individual file recommendations for flagged files
        for (FileItem f : activeFiles) {
            if (f.isProtected()) continue;

            boolean isFlagged = (f.getJunkScore() != null && f.getJunkScore() >= 25)
                    || "duplicate".equalsIgnoreCase(f.getCategory())
                    || "blurry".equalsIgnoreCase(f.getCategory())
                    || "large".equalsIgnoreCase(f.getCategory())
                    || "inactive".equalsIgnoreCase(f.getCategory())
                    || f.isTemporary();

            if (!isFlagged) continue;
            if ("Keep Original".equalsIgnoreCase(f.getRecommendationStatus())) continue;

            Recommendation r = new Recommendation();
            r.setUserId(userId);
            r.setTitle("Clean " + f.getName());
            r.setCategory(mapCategoryName(f.getCategory()));
            r.setType(f.getCategory() != null ? f.getCategory() : "junk");
            r.setFileId(f.getId());
            r.setFileName(f.getName());
            r.setFileType(f.getFileType() != null ? f.getFileType() : "other");
            r.setFileSize(f.getSizeBytes());
            r.setStorageSource(f.getStorageType() != null ? f.getStorageType() : "local");
            r.setStorageLocation(f.getStorageLocation());
            r.setJunkScore(f.getJunkScore() != null ? f.getJunkScore() : 30);
            r.setRiskLevel(f.getRiskCategory() != null ? f.getRiskCategory() : "medium");
            r.setSafetyLevel("low".equalsIgnoreCase(f.getRiskCategory()) ? "Safe to Delete" : "Review Recommended");
            r.setSafetyScore(Math.max(10, 100 - r.getJunkScore()));
            r.setPotentialSavingsBytes(f.getSizeBytes());
            r.setFilesCount(1);
            r.setTargetFileIds(List.of(f.getId()));
            r.setAnalysisDate(f.getUpdatedAt() != null ? f.getUpdatedAt() : Instant.now());
            r.setRecommendedAction(f.getRecommendationStatus() != null ? f.getRecommendationStatus() : "Review & Delete");
            r.setSuggestedAction(r.getRecommendedAction() + " to reclaim storage safely");

            List<String> reasons = new ArrayList<>();
            if (f.getDetectionReason() != null && !f.getDetectionReason().isBlank()) {
                reasons.add(f.getDetectionReason());
            } else {
                reasons.add("Junk score: " + r.getJunkScore() + "/100");
            }
            r.setDetectionReasons(reasons);
            r.setDescription(String.join(". ", reasons));
            r.setStatus("pending");
            r.setProtected(f.isProtected());
            r.setExcluded(f.isExcluded());

            generated.add(r);
        }

        // 2. Also generate grouped high-level recipes for macro cleanup actions
        createGroupedMacroRecipes(userId, activeFiles, generated);

        if (generated.isEmpty()) {
            return generateDefaultRecommendations(userId);
        }

        recommendationRepository.saveAll(generated);

        // Notify user about newly generated recommendations
        notificationService.createNotification(
                userId,
                "New Cleanup Recommendations Ready",
                String.format("Generated %d optimization recommendations based on your scan.", generated.size()),
                "new_recommendations",
                "/recommendations"
        );

        return generated;
    }

    private void createGroupedMacroRecipes(String userId, List<FileItem> activeFiles, List<Recommendation> targetList) {
        // Group duplicate candidate copies
        List<FileItem> duplicateCopies = activeFiles.stream()
                .filter(f -> !f.isProtected() && "duplicate".equalsIgnoreCase(f.getCategory()) && !"Keep Original".equalsIgnoreCase(f.getRecommendationStatus()))
                .toList();

        if (!duplicateCopies.isEmpty()) {
            long dupBytes = duplicateCopies.stream().mapToLong(FileItem::getSizeBytes).sum();
            Recommendation rec = new Recommendation();
            rec.setUserId(userId);
            rec.setTitle("Purge " + duplicateCopies.size() + " Exact Duplicate Replicas");
            rec.setCategory("Duplicates");
            rec.setType("duplicate");
            rec.setPriority("high");
            rec.setSafetyLevel("Safe to Delete");
            rec.setSafetyScore(98);
            rec.setPotentialSavingsBytes(dupBytes);
            rec.setFilesCount(duplicateCopies.size());
            rec.setRecommendedAction("Safe to Delete");
            rec.setRiskLevel("low");
            rec.setSuggestedAction("Keep master copies and delete redundant clones");
            rec.setStorageLocation("Multiple directories");
            rec.setTargetFileIds(duplicateCopies.stream().map(FileItem::getId).toList());
            rec.setDescription("Cryptographic SHA-256 byte-for-byte exact copies identified. Canonical originals are safely preserved.");
            rec.setStatus("pending");
            targetList.add(rec);
        }

        // Group blurry photos
        List<FileItem> blurryPhotos = activeFiles.stream()
                .filter(f -> !f.isProtected() && "blurry".equalsIgnoreCase(f.getCategory()))
                .toList();

        if (!blurryPhotos.isEmpty()) {
            long blurBytes = blurryPhotos.stream().mapToLong(FileItem::getSizeBytes).sum();
            Recommendation rec = new Recommendation();
            rec.setUserId(userId);
            rec.setTitle("Review " + blurryPhotos.size() + " Blurry & Degraded Photos");
            rec.setCategory("Blurry Images");
            rec.setType("blurry");
            rec.setPriority("medium");
            rec.setSafetyLevel("Review Recommended");
            rec.setSafetyScore(78);
            rec.setPotentialSavingsBytes(blurBytes);
            rec.setFilesCount(blurryPhotos.size());
            rec.setRecommendedAction("Review & Delete");
            rec.setRiskLevel("medium");
            rec.setSuggestedAction("Inspect gallery previews and discard defective photos");
            rec.setStorageLocation("Photo directories");
            rec.setTargetFileIds(blurryPhotos.stream().map(FileItem::getId).toList());
            rec.setDescription("Photos scored below clarity threshold using Laplacian variance. User visual inspection recommended before removal.");
            rec.setStatus("pending");
            targetList.add(rec);
        }
    }

    private String mapCategoryName(String cat) {
        if (cat == null) return "General Junk";
        return switch (cat.toLowerCase()) {
            case "duplicate" -> "Duplicates";
            case "blurry" -> "Blurry Images";
            case "large" -> "Large Files";
            case "inactive" -> "Inactive Files";
            case "temporary" -> "Temporary Files";
            default -> "General Junk";
        };
    }

    public List<Recommendation> generateDefaultRecommendations(String userId) {
        List<Recommendation> recs = new ArrayList<>();

        Recommendation r1 = new Recommendation();
        r1.setUserId(userId);
        r1.setTitle("Purge 1,482 Exact Duplicate Files");
        r1.setCategory("Duplicates");
        r1.setType("duplicate");
        r1.setPriority("high");
        r1.setSafetyLevel("Safe to Delete");
        r1.setSafetyScore(98);
        r1.setPotentialSavingsBytes(95780000000L); // ~89.2 GB
        r1.setFilesCount(1482);
        r1.setDescription("Identified byte-for-byte MD5 and SHA-256 matches across multiple download folders. Preserves the canonical original copy.");
        r1.setSuggestedAction("Keep original copy and delete 1,482 redundant replicas");
        r1.setStorageLocation("Local: C:/Users/rocky/Downloads & Captures");
        r1.setRiskLevel("low");
        r1.setRecommendedAction("Safe to Delete");
        r1.setStatus("pending");

        Recommendation r2 = new Recommendation();
        r2.setUserId(userId);
        r2.setTitle("Archive 185 Giant Media & ISO Files (>500MB)");
        r2.setCategory("Large Files");
        r2.setType("large");
        r2.setPriority("high");
        r2.setSafetyLevel("Review Recommended");
        r2.setSafetyScore(84);
        r2.setPotentialSavingsBytes(121120000000L); // ~112.8 GB
        r2.setFilesCount(185);
        r2.setDescription("Stale disk images, high-bitrate OBS screen recordings and video renders unaccessed for over 90 days.");
        r2.setSuggestedAction("Compress or move to cold storage tier");
        r2.setStorageLocation("Local: D:/Projects/Archive");
        r2.setRiskLevel("medium");
        r2.setRecommendedAction("Recommended for Archival");
        r2.setStatus("pending");

        Recommendation r3 = new Recommendation();
        r3.setUserId(userId);
        r3.setTitle("Review 348 Blurry & Out-of-Focus Photos");
        r3.setCategory("Blurry Images");
        r3.setType("blurry");
        r3.setPriority("medium");
        r3.setSafetyLevel("Review Recommended");
        r3.setSafetyScore(78);
        r3.setPotentialSavingsBytes(15570000000L); // ~14.5 GB
        r3.setFilesCount(348);
        r3.setDescription("Computer vision Laplacian variance scored these photos below the 0.85 clarity threshold.");
        r3.setSuggestedAction("Batch discard after quick side-by-side inspection");
        r3.setStorageLocation("Local: C:/Users/rocky/Pictures/Imports");
        r3.setRiskLevel("medium");
        r3.setRecommendedAction("Review & Delete");
        r3.setStatus("pending");

        return recommendationRepository.saveAll(recs);
    }
}
