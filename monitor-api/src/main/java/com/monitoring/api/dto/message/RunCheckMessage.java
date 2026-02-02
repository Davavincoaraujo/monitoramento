package com.monitoring.api.dto.message;

public record RunCheckMessage(
    Long siteId,
    String siteName,
    String baseUrl
) {}
