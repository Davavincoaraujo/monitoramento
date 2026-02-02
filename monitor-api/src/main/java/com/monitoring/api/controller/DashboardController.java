package com.monitoring.api.controller;

import com.monitoring.api.dto.dashboard.*;
import com.monitoring.api.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> getOverview(
            @RequestParam Long siteId,
            @RequestParam(defaultValue = "24h") String range) {
        return ResponseEntity.ok(dashboardService.getOverview(siteId, range));
    }
    
    @GetMapping("/timeseries/errors")
    public ResponseEntity<TimeseriesResponse> getErrorTimeseries(
            @RequestParam Long siteId,
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(defaultValue = "1h") String bucket) {
        return ResponseEntity.ok(dashboardService.getErrorTimeseries(siteId, range, bucket));
    }
    
    @GetMapping("/timeseries/perf")
    public ResponseEntity<TimeseriesResponse> getPerfTimeseries(
            @RequestParam Long siteId,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(defaultValue = "1h") String bucket) {
        return ResponseEntity.ok(dashboardService.getPerfTimeseries(siteId, range, bucket));
    }
}
