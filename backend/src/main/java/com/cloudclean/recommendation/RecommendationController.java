package com.cloudclean.recommendation;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.FileItemResponse;
import com.cloudclean.dto.RecommendationResponse;
import com.cloudclean.dto.RecommendationUpdateRequest;
import com.cloudclean.file.FileItem;
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
@Tag(name = "Cleanup Recommendations", description = "Endpoints for prioritized storage cleanup advice, accept/reject, exclusion, and file protection")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(summary = "Get filtered recommendations", description = "Retrieves prioritized AI cleanup recommendations with safety scores and filters")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minScore) {
        String userId = SecurityUtils.getCurrentUserId();
        List<RecommendationResponse> recommendations = recommendationService.getUserRecommendations(userId, category, riskLevel, status, minScore);
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

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept recommendation", description = "Accepts the recommendation for cleanup processing")
    public ResponseEntity<ApiResponse<RecommendationResponse>> acceptRecommendation(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        RecommendationResponse accepted = recommendationService.acceptRecommendation(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Recommendation accepted", accepted));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject recommendation", description = "Dismisses/rejects recommendation from cleanup")
    public ResponseEntity<ApiResponse<RecommendationResponse>> rejectRecommendation(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        RecommendationResponse rejected = recommendationService.rejectRecommendation(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Recommendation rejected", rejected));
    }

    @PostMapping("/{id}/exclude")
    @Operation(summary = "Exclude file from recommendations", description = "Excludes file from being suggested in cleanup recommendations")
    public ResponseEntity<ApiResponse<RecommendationResponse>> excludeFile(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        RecommendationResponse excluded = recommendationService.excludeFile(userId, id);
        return ResponseEntity.ok(ApiResponse.success("File excluded from recommendations", excluded));
    }

    @PostMapping("/files/{fileId}/protect")
    @Operation(summary = "Protect important file", description = "Whitelists a file, preventing it from ever being marked as junk or deleted")
    public ResponseEntity<ApiResponse<FileItemResponse>> protectFile(@PathVariable String fileId) {
        String userId = SecurityUtils.getCurrentUserId();
        FileItem protectedFile = recommendationService.protectFile(userId, fileId);
        return ResponseEntity.ok(ApiResponse.success("File protected and whitelisted safely", new FileItemResponse(protectedFile)));
    }

    @DeleteMapping("/files/{fileId}/protect")
    @Operation(summary = "Remove protection from file", description = "Removes whitelisting protection from file")
    public ResponseEntity<ApiResponse<FileItemResponse>> unprotectFile(@PathVariable String fileId) {
        String userId = SecurityUtils.getCurrentUserId();
        FileItem file = recommendationService.unprotectFile(userId, fileId);
        return ResponseEntity.ok(ApiResponse.success("File protection removed", new FileItemResponse(file)));
    }

    @PostMapping("/generate")
    @Operation(summary = "Regenerate recommendations", description = "Recalculates recommendations based on latest database detection results")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> regenerateRecommendations() {
        String userId = SecurityUtils.getCurrentUserId();
        List<Recommendation> generated = recommendationService.generateRecommendationsFromDetections(userId);
        List<RecommendationResponse> responses = generated.stream().map(RecommendationResponse::new).toList();
        return ResponseEntity.ok(ApiResponse.success("Recommendations successfully updated from latest detection scan", responses));
    }
}
