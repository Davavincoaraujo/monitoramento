package com.monitoring.api.dto.report;

public record SlowPage(
    String pageName,
    Integer avgLoadMs
) {}
