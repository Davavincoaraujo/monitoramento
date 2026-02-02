package com.monitoring.api.dto.ingest;

import com.monitoring.api.domain.enums.FailureType;
import com.monitoring.api.domain.enums.Severity;
import jakarta.validation.constraints.NotNull;

public record FailureDTO(
    Long pageId,
    @NotNull Severity severity,
    @NotNull FailureType type,
    @NotNull String message,
    String url
) {}
