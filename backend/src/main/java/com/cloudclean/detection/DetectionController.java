package com.cloudclean.detection;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.file.FileItem;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/detection", "/api/v1/detection"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Intelligent Detection Engine", description = "Endpoints for SHA-256 duplicate grouping, perceptual similarity, blur diagnostics, inactivity, large files, and junk scoring")
public class DetectionController {

    private final DuplicateDetector duplicateDetector;
    private final BlurDetector blurDetector;
    private final InactiveFileDetector inactiveFileDetector;
    private final LargeFileDetector largeFileDetector;
    private final TemporaryFileDetector temporaryFileDetector;
    private final JunkScoringEngine junkScoringEngine;

    public DetectionController(DuplicateDetector duplicateDetector,
                               BlurDetector blurDetector,
                               InactiveFileDetector inactiveFileDetector,
                               LargeFileDetector largeFileDetector,
                               TemporaryFileDetector temporaryFileDetector,
                               JunkScoringEngine junkScoringEngine) {
        this.duplicateDetector = duplicateDetector;
        this.blurDetector = blurDetector;
        this.inactiveFileDetector = inactiveFileDetector;
        this.largeFileDetector = largeFileDetector;
        this.temporaryFileDetector = temporaryFileDetector;
        this.junkScoringEngine = junkScoringEngine;
    }

    @PostMapping("/duplicates")
    @Operation(summary = "Detect duplicate files", description = "Executes size-first SHA-256 duplicate grouping and identifies canonical originals vs copies")
    public ResponseEntity<ApiResponse<DuplicateDetector.DuplicateDetectionReport>> detectDuplicates() {
        String userId = SecurityUtils.getCurrentUserId();
        DuplicateDetector.DuplicateDetectionReport report = duplicateDetector.detectDuplicates(userId);
        return ResponseEntity.ok(ApiResponse.success("Duplicate files identified", report));
    }

    @PostMapping("/similar-images")
    @Operation(summary = "Detect visually similar images", description = "Uses perceptual hashing (dHash/aHash) to identify visually similar images as a separate feature")
    public ResponseEntity<ApiResponse<List<DuplicateDetector.SimilarImageGroup>>> detectSimilarImages(
            @RequestParam(defaultValue = "5") int maxDistance) {
        String userId = SecurityUtils.getCurrentUserId();
        List<DuplicateDetector.SimilarImageGroup> groups = duplicateDetector.detectVisuallySimilarImages(userId, maxDistance);
        return ResponseEntity.ok(ApiResponse.success("Visually similar image clusters identified", groups));
    }

    @PostMapping("/blurry")
    @Operation(summary = "Analyze blurry images", description = "Evaluates image sharpness using variance of Laplacian via Python microservice with Java fallback")
    public ResponseEntity<ApiResponse<BlurDetector.BlurAnalysisReport>> analyzeBlurry(
            @RequestParam(defaultValue = "0.70") double blurThreshold) {
        String userId = SecurityUtils.getCurrentUserId();
        BlurDetector.BlurAnalysisReport report = blurDetector.analyzeImages(userId, blurThreshold);
        return ResponseEntity.ok(ApiResponse.success("Blurry image analysis completed", report));
    }

    @PostMapping("/inactive")
    @Operation(summary = "Audit inactive files", description = "Categorizes dormant files into 30/90/180/365-day buckets based on access and modification dates")
    public ResponseEntity<ApiResponse<InactiveFileDetector.InactivityReport>> auditInactive(
            @RequestParam(defaultValue = "180") int daysThreshold) {
        String userId = SecurityUtils.getCurrentUserId();
        InactiveFileDetector.InactivityReport report = inactiveFileDetector.detectInactiveFiles(userId, daysThreshold);
        return ResponseEntity.ok(ApiResponse.success("Inactive files audit completed", report));
    }

    @PostMapping("/large")
    @Operation(summary = "Detect large files", description = "Surfaces oversized images, videos, archives, and documents based on category thresholds")
    public ResponseEntity<ApiResponse<LargeFileDetector.LargeFileReport>> detectLarge(
            @RequestBody(required = false) LargeFileDetector.LargeFileThresholds thresholds) {
        String userId = SecurityUtils.getCurrentUserId();
        LargeFileDetector.LargeFileReport report = largeFileDetector.detectLargeFiles(userId, thresholds);
        return ResponseEntity.ok(ApiResponse.success("Large file audit completed", report));
    }

    @PostMapping("/temporary")
    @Operation(summary = "Detect temporary files", description = "Evaluates temporary extensions and directories with safety rules against automatic deletion")
    public ResponseEntity<ApiResponse<TemporaryFileDetector.TemporaryReport>> detectTemporary() {
        String userId = SecurityUtils.getCurrentUserId();
        TemporaryFileDetector.TemporaryReport report = temporaryFileDetector.detectTemporaryFiles(userId);
        return ResponseEntity.ok(ApiResponse.success("Temporary files evaluated", report));
    }

    @PostMapping("/score-junk")
    @Operation(summary = "Calculate composite junk scores", description = "Runs rule-based scoring engine (0-100), assigning risk categories and recommendations without double-counting")
    public ResponseEntity<ApiResponse<List<FileItem>>> scoreAllFiles() {
        String userId = SecurityUtils.getCurrentUserId();
        List<FileItem> scoredFiles = junkScoringEngine.scoreAllUserFiles(userId);
        return ResponseEntity.ok(ApiResponse.success("Junk scoring evaluation complete", scoredFiles));
    }
}
