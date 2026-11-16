package com.cloudclean.report;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/reports", "/api/v1/reports"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports & Analytics", description = "Endpoints for storage reports, scan audits, category distribution, and CSV export")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get reports overview", description = "Retrieves storage metrics, category distribution, and summaries of scans and cleanups")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportsOverview() {
        String userId = SecurityUtils.getCurrentUserId();
        Map<String, Object> data = reportService.getReportsOverview(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export data to CSV", description = "Exports files, cleanup audit logs, or scan history to standard RFC 4180 CSV format")
    public ResponseEntity<byte[]> exportToCsv(@RequestParam(defaultValue = "files") String type) {
        String userId = SecurityUtils.getCurrentUserId();
        String csvContent;
        String fileName;

        switch (type.toLowerCase()) {
            case "cleanup":
                csvContent = reportService.exportCleanupHistoryToCsv(userId);
                fileName = "cloudclean_cleanup_audit.csv";
                break;
            case "scans":
                csvContent = reportService.exportScanHistoryToCsv(userId);
                fileName = "cloudclean_scan_history.csv";
                break;
            default:
                csvContent = reportService.exportFilesToCsv(userId);
                fileName = "cloudclean_inventory.csv";
                break;
        }

        byte[] bytes = csvContent.getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
