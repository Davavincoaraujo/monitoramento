package com.monitoring.api.dto.report;

public record TopIssue(
    String type,
    String message,
    Integer count
) {}
