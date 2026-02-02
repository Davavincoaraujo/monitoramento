package com.monitoring.api.controller;

import com.monitoring.api.dto.dashboard.*;
import com.monitoring.api.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller para endpoints do Dashboard de monitoramento.
 * 
 * <p>Fornece dados agregados e métricas para visualização no dashboard web.
 * Todos os endpoints retornam dados processados e prontos para consumo
 * pelos componentes frontend (gráficos, cards, tabelas).</p>
 * 
 * <p><b>Endpoints disponíveis:</b></p>
 * <pre>
 * GET /api/dashboard/overview              - Overview com métricas agregadas
 * GET /api/dashboard/timeseries/errors     - Série temporal de erros
 * GET /api/dashboard/timeseries/perf       - Série temporal de performance
 * </pre>
 * 
 * <p><b>Parâmetros comuns:</b></p>
 * <ul>
 *   <li><b>siteId:</b> ID do site a ser analisado (obrigatório)</li>
 *   <li><b>range:</b> Período de análise - 1h, 6h, 24h, 7d, 30d (padrão: 24h)</li>
 *   <li><b>bucket:</b> Intervalo de agrupamento - 5m, 1h, 6h, 1d (padrão: 1h)</li>
 * </ul>
 * 
 * <p><b>Exemplo de uso:</b></p>
 * <pre>
 * GET /api/dashboard/overview?siteId=1&range=24h
 * GET /api/dashboard/timeseries/errors?siteId=1&range=7d&bucket=6h
 * </pre>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see DashboardService
 * @see OverviewResponse
 * @see TimeseriesResponse
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @GetMapping("/overview")
    public ResponseEntity<OverviewResponse> getOverview(
            @RequestParam Long siteId,
            @RequestParam(defaultValue = "24h") String range) {
        return ResponseEntity.ok(dashboardService.getOverview(siteId, range));
    }
    
    @GetMapping("/timeseries/errors")
    public ResponseEntity<TimeseriesResponse> getErrorTimeseries(
            @RequestParam Long siteId,
            @RequestParam(defaultValue = "24h") String range,
            @RequestParam(defaultValue = "1h") String bucket) {
        return ResponseEntity.ok(dashboardService.getErrorTimeseries(siteId, range, bucket));
    }
    
    @GetMapping("/timeseries/perf")
    public ResponseEntity<TimeseriesResponse> getPerfTimeseries(
            @RequestParam Long siteId,
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(defaultValue = "1h") String bucket) {
        return ResponseEntity.ok(dashboardService.getPerfTimeseries(siteId, range, bucket));
    }
}
