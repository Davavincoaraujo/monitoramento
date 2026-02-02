package com.monitoring.api.dto.ingest;

public record IngestRunResponse(
    Long runId,
    String status,
    String message
) {}
