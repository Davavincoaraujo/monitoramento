package com.monitoring.api.controller;

import com.monitoring.api.domain.entity.Run;
import com.monitoring.api.domain.repository.RunRepository;
import com.monitoring.api.dto.dashboard.RunSummaryDTO;
import com.monitoring.api.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/runs")
public class RunController {
    
    private final RunRepository runRepository;
    private final DashboardService dashboardService;
    
    public RunController(RunRepository runRepository, DashboardService dashboardService) {
        this.runRepository = runRepository;
        this.dashboardService = dashboardService;
    }
    
    @GetMapping
    public ResponseEntity<List<RunSummaryDTO>> getRuns(
            @RequestParam Long siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(dashboardService.getRuns(siteId, from, to));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Run> getRun(@PathVariable Long id) {
        return runRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
