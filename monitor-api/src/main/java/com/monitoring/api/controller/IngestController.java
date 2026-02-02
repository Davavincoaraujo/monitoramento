package com.monitoring.api.controller;

import com.monitoring.api.dto.ingest.IngestRunRequest;
import com.monitoring.api.dto.ingest.IngestRunResponse;
import com.monitoring.api.service.IngestService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller para ingestão de resultados de runs (usado pelo monitor-runner).
 * 
 * <p><b>Rate Limiting:</b> Endpoint protegido com Resilience4j RateLimiter:</p>
 * <ul>
 *   <li>Limite: 10 requisições por minuto</li>
 *   <li>Refresh: 60 segundos</li>
 *   <li>Exceção: RequestNotPermitted → 429 Too Many Requests</li>
 * </ul>
 * 
 * <p><b>Endpoint:</b></p>
 * <pre>
 * POST /api/ingest/runs
 * Content-Type: application/json
 * 
 * Body: IngestRunRequest {
 *   siteId, startedAt, endedAt, status,
 *   pageResults[], failures[], requestErrors[]
 * }
 * 
 * Response: 200 OK → IngestRunResponse { runId }
 *           429 Too Many Requests → Rate limit exceeded
 *           400 Bad Request → Validação falhou
 * </pre>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see IngestService
 * @see IngestRunRequest
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestController {
    
    private final IngestService ingestService;
    
    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }
    
    /**
     * Ingere resultados de uma execução (run) enviados pelo monitor-runner.
     * 
     * <p><b>Rate Limit:</b> 10 requisições por minuto.</p>
     * 
     * @param request Dados completos da run com resultados, falhas e erros
     * @return Response com ID da run criada
     * @throws io.github.resilience4j.ratelimiter.RequestNotPermitted se exceder rate limit
     */
    @PostMapping("/runs")
    @RateLimiter(name = "ingest", fallbackMethod = "ingestRateLimitFallback")
    public ResponseEntity<IngestRunResponse> ingestRun(@Valid @RequestBody IngestRunRequest request) {
        IngestRunResponse response = ingestService.ingestRun(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Fallback method quando rate limit é excedido.
     * 
     * @param request Request original (não utilizado)
     * @param ex Exceção do rate limiter
     * @return Response 429 Too Many Requests
     */
    @SuppressWarnings("unused")
    private ResponseEntity<IngestRunResponse> ingestRateLimitFallback(
            IngestRunRequest request, 
            Exception ex) {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new IngestRunResponse(null, "Rate limit exceeded. Max 10 requests per minute."));
    }
}

