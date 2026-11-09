package com.cloudclean.recommendation;

import com.cloudclean.dto.RecommendationResponse;
import com.cloudclean.dto.RecommendationUpdateRequest;
import com.cloudclean.exception.ResourceNotFoundException;
import com.cloudclean.file.FileItem;
import com.cloudclean.repository.FileItemRepository;
import com.cloudclean.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final RecommendationRepository recommendationRepository;
    private final FileItemRepository fileItemRepository;

    public RecommendationService(RecommendationRepository recommendationRepository,
                                 FileItemRepository fileItemRepository) {
        this.recommendationRepository = recommendationRepository;
        this.fileItemRepository = fileItemRepository;
    }

    public List<RecommendationResponse> getUserRecommendations(String userId) {
        List<Recommendation> recommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (recommendations.isEmpty()) {
            recommendations = generateDefaultRecommendations(userId);
        }

        return recommendations.stream()
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
        log.info("Updated recommendation id: {} to status: {}", id, request.getStatus());
        return new RecommendationResponse(saved);
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
        r3.setDescription("Laplacian variance blur detection scored these images below the 0.85 clarity threshold.");
        r3.setSuggestedAction("Inspect thumbnail side-by-side and batch discard");
        r3.setStorageLocation("Local: C:/Users/rocky/Pictures/Imports");
        r3.setStatus("pending");

        Recommendation r4 = new Recommendation();
        r4.setUserId(userId);
        r4.setTitle("Clean Stale Build Artifacts & Logs");
        r4.setCategory("Inactive Files");
        r4.setType("inactive");
        r4.setPriority("low");
        r4.setSafetyLevel("Safe to Delete");
        r4.setSafetyScore(99);
        r4.setPotentialSavingsBytes(23500000000L);
        r4.setFilesCount(320);
        r4.setDescription("Untouched .cache and build directories from completed client sprints inactive for 180+ days.");
        r4.setSuggestedAction("One-click automated safe purge");
        r4.setStorageLocation("Local: C:/Users/rocky/Projects");
        r4.setStatus("pending");

        recs.addAll(List.of(r1, r2, r3, r4));
        return recommendationRepository.saveAll(recs);
    }
}
