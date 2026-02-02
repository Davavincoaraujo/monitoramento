package com.monitoring.api.dto.dashboard;

import java.time.LocalDateTime;

public record DataPoint(
    LocalDateTime timestamp,
    Double value,
    String label
) {}
