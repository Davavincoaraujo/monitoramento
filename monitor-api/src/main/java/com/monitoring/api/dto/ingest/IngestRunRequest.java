package com.monitoring.api.dto.ingest;

import com.monitoring.api.domain.enums.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record IngestRunRequest(
    @NotNull Long siteId,
    @NotNull LocalDateTime startedAt,
    LocalDateTime endedAt,
    @NotNull RunStatus status,
    String summary,
    @Valid List<PageResultDTO> pageResults,
    @Valid List<FailureDTO> failures,
    @Valid List<RequestErrorDTO> requestErrors
) {}
