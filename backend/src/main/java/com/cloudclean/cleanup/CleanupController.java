package com.cloudclean.cleanup;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.CleanupHistoryResponse;
import com.cloudclean.dto.CleanupImpactResponse;
import com.cloudclean.dto.CleanupRequest;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/cleanup", "/api/v1/cleanup"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cleanup Execution & Audit", description = "Endpoints for executing validated file purges, impact analysis, and restoration")
public class CleanupController {

    private final CleanupService cleanupService;

    public CleanupController(CleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @PostMapping("/impact")
    @Operation(summary = "Calculate cleanup impact", description = "Analyzes candidate files, calculates estimated recoverable bytes, checks for protected files, and returns preview warning")
    public ResponseEntity<ApiResponse<CleanupImpactResponse>> calculateImpact(@RequestBody CleanupRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        CleanupImpactResponse impact = cleanupService.calculateCleanupImpact(userId, request);
        return ResponseEntity.ok(ApiResponse.success(impact));
    }

    @PostMapping
    @Operation(summary = "Execute verified cleanup", description = "Executes file purges with mandatory explicit confirmation check and provider trash operations")
    public ResponseEntity<ApiResponse<CleanupHistoryResponse>> executeCleanup(@Valid @RequestBody CleanupRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        String userEmail = SecurityUtils.getCurrentUserEmail();
        CleanupHistoryResponse response = cleanupService.executeCleanup(userId, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Cleanup operation executed successfully", response));
    }

    @PostMapping("/history/{id}/restore")
    @Operation(summary = "Restore cleaned files", description = "Restores files moved to trash back to their original storage location")
    public ResponseEntity<ApiResponse<CleanupHistoryResponse>> restoreCleanup(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        CleanupHistoryResponse response = cleanupService.restoreCleanup(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Files successfully restored from trash", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get cleanup history", description = "Returns cryptographically timestamped audit log of past cleanup actions and restoration status")
    public ResponseEntity<ApiResponse<PageResponse<CleanupHistoryResponse>>> getCleanupHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        PageResponse<CleanupHistoryResponse> history = cleanupService.getCleanupHistory(userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
