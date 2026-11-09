package com.cloudclean.cleanup;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.CleanupHistoryResponse;
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
@Tag(name = "Cleanup Execution & Audit", description = "Endpoints for executing validated file purges and reviewing audit history")
public class CleanupController {

    private final CleanupService cleanupService;

    public CleanupController(CleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @PostMapping
    @Operation(summary = "Execute verified cleanup", description = "Executes file purges or recommendation actions with mandatory explicit confirmation check")
    public ResponseEntity<ApiResponse<CleanupHistoryResponse>> executeCleanup(@Valid @RequestBody CleanupRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        String userEmail = SecurityUtils.getCurrentUserEmail();
        CleanupHistoryResponse response = cleanupService.executeCleanup(userId, request, userEmail);
        return ResponseEntity.ok(ApiResponse.success("Cleanup operation executed successfully", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get cleanup history", description = "Returns cryptographically timestamped audit log of past cleanup actions")
    public ResponseEntity<ApiResponse<PageResponse<CleanupHistoryResponse>>> getCleanupHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        PageResponse<CleanupHistoryResponse> history = cleanupService.getCleanupHistory(userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
