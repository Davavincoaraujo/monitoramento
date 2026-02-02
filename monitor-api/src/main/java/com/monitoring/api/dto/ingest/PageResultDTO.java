package com.monitoring.api.dto.ingest;

import jakarta.validation.constraints.NotNull;

public record PageResultDTO(
    @NotNull Long pageId,
    String finalUrl,
    Integer ttfbMs,
    Integer domMs,
    Integer loadMs,
    Integer requestsCount,
    Long totalBytes
) {}
