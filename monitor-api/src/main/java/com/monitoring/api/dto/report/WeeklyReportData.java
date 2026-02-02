package com.monitoring.api.dto.report;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyReportData(
    String siteName,
    LocalDate weekStart,
    LocalDate weekEnd,
    Double uptimePercent,
    Map<String, Integer> failuresBySeverity,
    PerformanceData performance,
    PerformanceData previousWeekPerformance,
    List<TopIssue> topIssues,
    List<SlowPage> slowestPages,
    List<AssetError> top404Assets,
    String dashboardLink
) {}
