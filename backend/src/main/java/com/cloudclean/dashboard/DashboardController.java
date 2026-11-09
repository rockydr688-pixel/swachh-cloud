package com.cloudclean.dashboard;

import com.cloudclean.dto.ApiResponse;
import com.cloudclean.dto.DashboardMetricsResponse;
import com.cloudclean.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/dashboard", "/api/v1/dashboard"})
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dashboard Telemetry", description = "Endpoints for aggregated storage KPIs, capacity meters, and optimization metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get storage dashboard metrics", description = "Returns aggregated storage capacity, recoverable bloat, file counts, and charts")
    public ResponseEntity<ApiResponse<DashboardMetricsResponse>> getDashboardMetrics() {
        String userId = SecurityUtils.getCurrentUserId();
        DashboardMetricsResponse metrics = dashboardService.getDashboardMetrics(userId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}
