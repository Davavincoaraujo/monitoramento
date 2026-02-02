package com.monitoring.api.dto.dashboard;

public record PerformanceMetrics(
    Integer p50LoadMs,
    Integer p95LoadMs,
    Integer p99LoadMs,
    Integer p95TtfbMs
) {}
