package com.monitoring.runner.dto;

public record RunCheckMessage(
    Long siteId,
    String siteName,
    String baseUrl
) {}
