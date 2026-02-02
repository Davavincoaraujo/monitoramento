package com.monitoring.api.controller;

import com.monitoring.api.domain.entity.Run;
import com.monitoring.api.domain.repository.RunRepository;
import com.monitoring.api.dto.dashboard.RunSummaryDTO;
import com.monitoring.api.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller para consulta de execuções (Runs) de monitoramento.
 * 
 * <p>Fornece endpoints para recuperar runs individuais ou listas de runs de um site.</p>
 * 
 * <p><b>Endpoints:</b></p>
 * <pre>
 * GET /api/runs?siteId={id}&from={date}&to={date}  - Lista runs de um site em período
 * GET /api/runs/{id}                                 - Detalhes completos de uma run
 * </pre>
 * 
 * <p><b>GET /api/runs (Lista Runs):</b></p>
 * <ul>
 *   <li>Retorna resumo de runs (RunSummaryDTO) sem coleções lazy</li>
 *   <li>Filtro obrigatório por siteId e período (from, to)</li>
 *   <li>Ordenação: por started_at DESC (mais recentes primeiro)</li>
 *   <li>Usado para histórico e gráficos no dashboard</li>
 * </ul>
 * 
 * <p><b>GET /api/runs/{id} (Detalhes da Run):</b></p>
 * <ul>
 *   <li>Retorna entidade Run completa com todas as coleções</li>
 *   <li>IMPORTANTE: Força hydration das coleções lazy (pageResults, failures, requestErrors)</li>
 *   <li>Evita LazyInitializationException durante serialização JSON</li>
 *   <li>Usado para visualizar detalhes completos de uma execução</li>
 * </ul>
 * 
 * <p><b>Lazy Loading Pattern:</b></p>
 * <pre>
 * // Força carregamento das coleções antes de serializar
 * run.getPageResults().size();    // Hydrate pageResults
 * run.getFailures().size();       // Hydrate failures
 * run.getRequestErrors().size();  // Hydrate requestErrors
 * 
 * Sem isso, Jackson lança exceção ao tentar serializar coleções lazy.
 * </pre>
 * 
 * <p><b>Formato de Data (ISO-8601):</b></p>
 * <pre>
 * @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
 * 
 * Exemplos aceitos:
 * - 2026-02-02T10:00:00
 * - 2026-02-02T10:00:00.123
 * - 2026-02-02T10:00:00+00:00
 * </pre>
 * 
 * <p><b>Exemplo de Uso:</b></p>
 * <pre>
 * // Lista últimas runs das últimas 24h
 * GET /api/runs?siteId=1&from=2026-02-01T00:00:00&to=2026-02-02T00:00:00
 * 
 * Response:
 * [
 *   {
 *     "id": 123,
 *     "status": "SUCCESS",
 *     "startedAt": "2026-02-02T09:00:00",
 *     "endedAt": "2026-02-02T09:00:15",
 *     "criticalCount": 0,
 *     "majorCount": 0,
 *     "minorCount": 1
 *   }
 * ]
 * 
 * // Detalhes completos de uma run
 * GET /api/runs/123
 * 
 * Response:
 * {
 *   "id": 123,
 *   "status": "SUCCESS",
 *   "pageResults": [...],
 *   "failures": [...],
 *   "requestErrors": [...]
 * }
 * </pre>
 * 
 * <p><b>Tratamento de Erros:</b></p>
 * <ul>
 *   <li>404 Not Found: Run com ID especificado não existe</li>
 *   <li>400 Bad Request: Parâmetros obrigatórios ausentes (siteId, from, to)</li>
 *   <li>400 Bad Request: Formato de data inválido</li>
 * </ul>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see Run
 * @see RunSummaryDTO
 * @see DashboardService
 */
@RestController
@RequestMapping("/api/runs")
public class RunController {
    
    private final RunRepository runRepository;
    private final DashboardService dashboardService;
    
    public RunController(RunRepository runRepository, DashboardService dashboardService) {
        this.runRepository = runRepository;
        this.dashboardService = dashboardService;
    }
    
    @GetMapping
    public ResponseEntity<List<RunSummaryDTO>> getRuns(
            @RequestParam Long siteId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(dashboardService.getRuns(siteId, from, to));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Run> getRun(@PathVariable Long id) {
        return runRepository.findById(id)
            .map(run -> {
                // Force lazy loading before serialization
                run.getPageResults().size();
                run.getFailures().size();
                run.getRequestErrors().size();
                return ResponseEntity.ok(run);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
