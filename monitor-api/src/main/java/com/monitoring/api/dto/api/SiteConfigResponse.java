package com.monitoring.api.dto.api;

public record SiteConfigResponse(
    Long siteId,
    String name,
    String baseUrl,
    java.util.List<PageConfigDTO> pages
) {}
