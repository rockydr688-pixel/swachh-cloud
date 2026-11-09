package com.cloudclean.scanner;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.PageResponse;
import com.cloudclean.dto.ScanJobResponse;
import com.cloudclean.dto.ScanRequest;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/scans", "/api/v1/scans"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Scanning & Diagnostics", description = "Endpoints to trigger, monitor, and cancel asynchronous storage diagnostic jobs")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping
    @Operation(summary = "Trigger storage scan", description = "Launches asynchronous multi-threaded heuristic scanning across filesystems")
    public ResponseEntity<ApiResponse<ScanJobResponse>> triggerScan(@RequestBody(required = false) ScanRequest request) {
        String userId = SecurityUtils.getCurrentUserId();
        if (request == null) {
            request = new ScanRequest();
        }
        ScanJobResponse response = scanService.triggerScan(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Scan job enqueued and started asynchronously", response));
    }

    @GetMapping
    @Operation(summary = "List scan history", description = "Returns paginated scan history and audit runs for the authenticated user")
    public ResponseEntity<ApiResponse<PageResponse<ScanJobResponse>>> getScans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        String userId = SecurityUtils.getCurrentUserId();
        PageResponse<ScanJobResponse> response = scanService.getUserScans(userId, page, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get scan job details", description = "Returns real-time progress and statistics of a specific scan job")
    public ResponseEntity<ApiResponse<ScanJobResponse>> getScanById(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        ScanJobResponse response = scanService.getScanById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel active scan job", description = "Aborts an in-progress scan job")
    public ResponseEntity<ApiResponse<ScanJobResponse>> cancelScan(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        ScanJobResponse response = scanService.cancelScan(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Scan job cancelled successfully", response));
    }
}
