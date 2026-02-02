package com.monitoring.api.dto.dashboard;

import com.monitoring.api.domain.enums.FailureType;
import com.monitoring.api.domain.enums.Severity;

public record FailureDTO(
    Long id,
    Severity severity,
    FailureType type,
    String message,
    String url,
    String pageName
) {}
