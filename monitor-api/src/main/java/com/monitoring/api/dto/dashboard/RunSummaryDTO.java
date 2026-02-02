package com.monitoring.api.dto.dashboard;

import com.monitoring.api.domain.enums.RunStatus;

import java.time.LocalDateTime;

public record RunSummaryDTO(
    Long id,
    Long siteId,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    RunStatus status,
    Integer criticalCount,
    Integer majorCount,
    Integer minorCount,
    String summary
) {}
