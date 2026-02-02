package com.monitoring.api.dto.ingest;

import jakarta.validation.constraints.NotNull;

public record RequestErrorDTO(
    @NotNull String resourceType,
    @NotNull String url,
    Integer status,
    Integer durationMs,
    String errorMessage
) {}
