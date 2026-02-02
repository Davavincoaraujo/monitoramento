package com.monitoring.api.service;

import com.monitoring.api.domain.entity.PageResult;
import com.monitoring.api.domain.entity.Run;
import com.monitoring.api.domain.entity.Site;
import com.monitoring.api.domain.repository.*;
import com.monitoring.api.dto.dashboard.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    private final SiteRepository siteRepository;
    private final RunRepository runRepository;
    private final PageResultRepository pageResultRepository;
    private final FailureRepository failureRepository;
    
    public DashboardService(
            SiteRepository siteRepository,
            RunRepository runRepository,
            PageResultRepository pageResultRepository,
            FailureRepository failureRepository) {
        this.siteRepository = siteRepository;
        this.runRepository = runRepository;
        this.pageResultRepository = pageResultRepository;
        this.failureRepository = failureRepository;
    }
    
    public OverviewResponse getOverview(Long siteId, String range) {
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        
        LocalDateTime from = parseRange(range);
        
        // Get uptime
        Object[] uptimeData = runRepository.calculateUptime(siteId, from);
        Double uptime = calculateUptime(uptimeData);
        
        // Get issues by severity
        List<Object[]> severityCounts = failureRepository.countBySeveritySince(siteId, from);
        Map<String, Integer> issuesBySeverity = severityCounts.stream()
            .collect(Collectors.toMap(
                arr -> arr[0].toString(),
                arr -> ((Number) arr[1]).intValue()
            ));
        
        // Get performance metrics
        List<PageResult> results = pageResultRepository.findPageResultsInRange(
            siteId, from, LocalDateTime.now());
        PerformanceMetrics performance = calculatePerformanceMetrics(results);
        
        // Get last run
        List<Run> runs = runRepository.findBySiteIdOrderByStartedAtDesc(siteId);
        LastRun lastRun = runs.isEmpty() ? null : new LastRun(
            runs.get(0).getId(),
            runs.get(0).getStartedAt(),
            runs.get(0).getStatus().name(),
            runs.get(0).getCriticalCount(),
            runs.get(0).getMajorCount(),
            runs.get(0).getMinorCount()
        );
        
        return new OverviewResponse(
            siteId,
            site.getName(),
            determineStatus(runs.isEmpty() ? null : runs.get(0)),
            uptime,
            issuesBySeverity,
            performance,
            lastRun
        );
    }
    
    public TimeseriesResponse getErrorTimeseries(Long siteId, String range, String bucket) {
        LocalDateTime from = parseRange(range);
        LocalDateTime to = LocalDateTime.now();
        
        List<Run> runs = runRepository.findRunsInRange(siteId, from, to);
        
        List<DataPoint> dataPoints = runs.stream()
            .map(r -> new DataPoint(
                r.getStartedAt(),
                (double) (r.getCriticalCount() + r.getMajorCount()),
                "Errors"
            ))
            .collect(Collectors.toList());
        
        return new TimeseriesResponse(siteId, from, to, bucket, dataPoints);
    }
    
    public TimeseriesResponse getPerfTimeseries(Long siteId, String range, String bucket) {
        LocalDateTime from = parseRange(range);
        LocalDateTime to = LocalDateTime.now();
        
        List<PageResult> results = pageResultRepository.findPageResultsInRange(siteId, from, to);
        
        List<DataPoint> dataPoints = results.stream()
            .filter(r -> r.getLoadMs() != null)
            .map(r -> new DataPoint(
                r.getCreatedAt(),
                r.getLoadMs().doubleValue(),
                "Load Time"
            ))
            .collect(Collectors.toList());
        
        return new TimeseriesResponse(siteId, from, to, bucket, dataPoints);
    }
    
    public List<RunSummaryDTO> getRuns(Long siteId, LocalDateTime from, LocalDateTime to) {
        List<Run> runs = runRepository.findRunsInRange(siteId, from, to);
        
        return runs.stream()
            .map(r -> new RunSummaryDTO(
                r.getId(),
                r.getSite().getId(),
                r.getStartedAt(),
                r.getEndedAt(),
                r.getStatus(),
                r.getCriticalCount(),
                r.getMajorCount(),
                r.getMinorCount(),
                r.getSummary()
            ))
            .collect(Collectors.toList());
    }
    
    private LocalDateTime parseRange(String range) {
        return switch (range) {
            case "1h" -> LocalDateTime.now().minusHours(1);
            case "6h" -> LocalDateTime.now().minusHours(6);
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusHours(24);
        };
    }
    
    private Double calculateUptime(Object[] data) {
        if (data == null || data.length < 2) return 100.0;
        long total = ((Number) data[0]).longValue();
        long success = ((Number) data[1]).longValue();
        return total == 0 ? 100.0 : (success * 100.0 / total);
    }
    
    private PerformanceMetrics calculatePerformanceMetrics(List<PageResult> results) {
        List<Integer> loadTimes = results.stream()
            .map(PageResult::getLoadMs)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());
        
        List<Integer> ttfbTimes = results.stream()
            .map(PageResult::getTtfbMs)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());
        
        return new PerformanceMetrics(
            percentile(loadTimes, 50),
            percentile(loadTimes, 95),
            percentile(loadTimes, 99),
            percentile(ttfbTimes, 95)
        );
    }
    
    private Integer percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return null;
        int index = (int) Math.ceil(values.size() * p / 100.0) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }
    
    private String determineStatus(Run lastRun) {
        if (lastRun == null) return "UNKNOWN";
        return switch (lastRun.getStatus()) {
            case SUCCESS -> "HEALTHY";
            case WARNING -> "DEGRADED";
            case FAILED -> "DOWN";
            default -> "UNKNOWN";
        };
    }
}
