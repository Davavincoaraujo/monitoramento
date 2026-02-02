package com.monitoring.api.dto.ingest;

/**
 * Response DTO para endpoint de ingestão de runs.
 * 
 * @param runId ID da run criada (null se falhou)
 * @param message Mensagem de sucesso ou erro
 */
public record IngestRunResponse(
    Long runId,
    String message
) {
    public IngestRunResponse(Long runId) {
        this(runId, "Run ingested successfully");
    }
}

