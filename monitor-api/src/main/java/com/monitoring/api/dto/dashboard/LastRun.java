package com.monitoring.api.dto.dashboard;

import java.time.LocalDateTime;

public record LastRun(
    Long runId,
    LocalDateTime startedAt,
    String status,
    Integer criticalCount,
    Integer majorCount,
    Integer minorCount
) {}
