package com.monitoring.api.dto.dashboard;

import java.util.Map;

public record OverviewResponse(
    Long siteId,
    String siteName,
    String status,
    Double uptimePercent,
    Map<String, Integer> issuesBySeverity,
    PerformanceMetrics performance,
    LastRun lastRun
) {}
