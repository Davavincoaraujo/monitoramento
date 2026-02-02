package com.monitoring.runner.dto;

import java.util.List;

public record SiteConfig(
    Long siteId,
    String name,
    String baseUrl,
    List<PageConfig> pages
) {}
