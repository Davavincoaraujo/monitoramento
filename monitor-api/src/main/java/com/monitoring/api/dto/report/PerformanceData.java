package com.monitoring.api.dto.report;

public record PerformanceData(
    Integer p95LoadMs,
    Integer p95TtfbMs
) {}
