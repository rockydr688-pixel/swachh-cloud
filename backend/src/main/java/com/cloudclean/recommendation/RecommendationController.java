package com.cloudclean.recommendation;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.RecommendationResponse;
import com.cloudclean.dto.RecommendationUpdateRequest;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/recommendations", "/api/v1/recommendations"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cleanup Recommendations", description = "Endpoints for prioritized storage cleanup advice and action tracking")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(summary = "Get all recommendations", description = "Retrieves prioritized AI cleanup recommendations with safety scores and potential space savings")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations() {
        String userId = SecurityUtils.getCurrentUserId();
        List<RecommendationResponse> recommendations = recommendationService.getUserRecommendations(userId);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get recommendation details", description = "Retrieves details of a specific cleanup recommendation")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendationById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        RecommendationResponse recommendation = recommendationService.getRecommendationById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(recommendation));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update recommendation status", description = "Updates status of recommendation (e.g. pending, applied, dismissed)")
    public ResponseEntity<ApiResponse<RecommendationResponse>> updateRecommendation(
            @PathVariable String id,
            @Valid @RequestBody RecommendationUpdateRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        RecommendationResponse updated = recommendationService.updateRecommendationStatus(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Recommendation status updated", updated));
    }
}
