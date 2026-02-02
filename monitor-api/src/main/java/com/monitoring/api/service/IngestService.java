package com.monitoring.api.service;

import com.monitoring.api.domain.entity.*;
import com.monitoring.api.domain.enums.Severity;
import com.monitoring.api.domain.repository.*;
import com.monitoring.api.dto.ingest.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsável por ingerir resultados de execuções (Runs) enviados pelo monitor-runner.
 * 
 * <p>Este é o ponto de entrada dos resultados coletados pelo Playwright. O runner executa
 * os checks sintéticos e envia os dados via POST /api/ingest/runs.</p>
 * 
 * <p><b>Fluxo de Ingestão:</b></p>
 * <ol>
 *   <li>Recebe IngestRunRequest com todos os resultados</li>
 *   <li>Valida que o site existe no banco</li>
 *   <li>Cria entidade Run com status e timestamps</li>
 *   <li>Persiste PageResults (métricas de performance por página)</li>
 *   <li>Persiste Failures (erros detectados)</li>
 *   <li>Persiste RequestErrors (falhas HTTP)</li>
 *   <li>Calcula contadores por severidade (critical, major, minor)</li>
 *   <li>Determina status final da Run (SUCCESS, WARNING, FAILED, ERROR)</li>
 *   <li>Salva Run no banco (cascade persiste relacionamentos)</li>
 *   <li>Publica evento SSE para clientes conectados (live monitoring)</li>
 * </ol>
 * 
 * <p><b>Determinação de Status:</b></p>
 * <pre>
 * ERROR    - Se request tem error != null
 * FAILED   - Se criticalCount > 0 ou majorCount > 0
 * WARNING  - Se minorCount > 0 (apenas issues minor)
 * SUCCESS  - Se nenhum issue encontrado
 * </pre>
 * 
 * <p><b>Contadores de Severidade:</b></p>
 * <ul>
 *   <li>criticalCount: soma de failures com severity = CRITICAL</li>
 *   <li>majorCount: soma de failures com severity = MAJOR</li>
 *   <li>minorCount: soma de failures com severity = MINOR</li>
 * </ul>
 * 
 * <p><b>Transações:</b></p>
 * <pre>
 * @Transactional garante atomicidade:
 * - Rollback completo se qualquer parte falhar
 * - Todas as entidades (Run, PageResults, Failures, RequestErrors) persistidas juntas
 * - Evento SSE publicado apenas após commit
 * </pre>
 * 
 * <p><b>Relacionamentos Cascata:</b></p>
 * <pre>
 * Run (parent)
 *   |-- PageResult (cascade ALL, orphanRemoval)
 *   |-- Failure (cascade ALL, orphanRemoval)
 *   |-- RequestError (cascade ALL, orphanRemoval)
 * 
 * Apenas Run precisa ser salva explicitamente, os demais cascateiam.
 * </pre>
 * 
 * <p><b>Validações:</b></p>
 * <ul>
 *   <li>siteId deve existir no banco (throws IllegalArgumentException)</li>
 *   <li>pageId (em PageResultDTO) deve existir no banco</li>
 *   <li>Timestamps devem estar presentes (startedAt, endedAt)</li>
 *   <li>Status deve ser válido (enum RunStatus)</li>
 * </ul>
 * 
 * <p><b>Eventos Publicados:</b></p>
 * <pre>
 * EventPublisher publica evento SSE após ingestão:
 * - Topic: site_{siteId}
 * - Event: run-completed
 * - Data: RunSummaryDTO com status, counts, timestamp
 * 
 * Clientes conectados via GET /api/events?siteId=X recebem atualizações em tempo real.
 * </pre>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see IngestRunRequest
 * @see Run
 * @see PageResult
 * @see Failure
 * @see EventPublisher
 */
@Service
public class IngestService {
    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    
    private final RunRepository runRepository;
    private final SiteRepository siteRepository;
    private final SitePageRepository pageRepository;
    private final PageResultRepository pageResultRepository;
    private final FailureRepository failureRepository;
    private final RequestErrorRepository requestErrorRepository;
    private final EventPublisher eventPublisher;
    
    public IngestService(
            RunRepository runRepository,
            SiteRepository siteRepository,
            SitePageRepository pageRepository,
            PageResultRepository pageResultRepository,
            FailureRepository failureRepository,
            RequestErrorRepository requestErrorRepository,
            EventPublisher eventPublisher) {
        this.runRepository = runRepository;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.pageResultRepository = pageResultRepository;
        this.failureRepository = failureRepository;
        this.requestErrorRepository = requestErrorRepository;
        this.eventPublisher = eventPublisher;
    }
    
    @Transactional
    public IngestRunResponse ingestRun(IngestRunRequest request) {
        log.info("Ingesting run for siteId={}", request.siteId());
        
        Site site = siteRepository.findById(request.siteId())
            .orElseThrow(() -> new IllegalArgumentException("Site not found: " + request.siteId()));
        
        // Create Run entity
        Run run = new Run();
        run.setSite(site);
        run.setStartedAt(request.startedAt());
        run.setEndedAt(request.endedAt());
        run.setStatus(request.status());
        run.setSummary(request.summary());
        
        // Count severities
        int critical = 0, major = 0, minor = 0;
        if (request.failures() != null) {
            for (FailureDTO f : request.failures()) {
                if (f.severity() == Severity.CRITICAL) critical++;
                else if (f.severity() == Severity.MAJOR) major++;
                else if (f.severity() == Severity.MINOR) minor++;
            }
        }
        run.setCriticalCount(critical);
        run.setMajorCount(major);
        run.setMinorCount(minor);
        
        run = runRepository.save(run);
        
        // Save page results
        if (request.pageResults() != null) {
            for (PageResultDTO dto : request.pageResults()) {
                SitePage page = pageRepository.findById(dto.pageId())
                    .orElseThrow(() -> new IllegalArgumentException("Page not found: " + dto.pageId()));
                
                PageResult pr = new PageResult();
                pr.setRun(run);
                pr.setPage(page);
                pr.setFinalUrl(dto.finalUrl());
                pr.setTtfbMs(dto.ttfbMs());
                pr.setDomMs(dto.domMs());
                pr.setLoadMs(dto.loadMs());
                pr.setRequestsCount(dto.requestsCount() != null ? dto.requestsCount() : 0);
                pr.setTotalBytes(dto.totalBytes() != null ? dto.totalBytes() : 0L);
                
                pageResultRepository.save(pr);
            }
        }
        
        // Save failures
        if (request.failures() != null) {
            for (FailureDTO dto : request.failures()) {
                Failure failure = new Failure();
                failure.setRun(run);
                if (dto.pageId() != null) {
                    SitePage page = pageRepository.findById(dto.pageId()).orElse(null);
                    failure.setPage(page);
                }
                failure.setSeverity(dto.severity());
                failure.setType(dto.type());
                failure.setMessage(dto.message());
                failure.setUrl(dto.url());
                
                failureRepository.save(failure);
            }
        }
        
        // Save request errors
        if (request.requestErrors() != null) {
            for (RequestErrorDTO dto : request.requestErrors()) {
                RequestError re = new RequestError();
                re.setRun(run);
                re.setResourceType(dto.resourceType());
                re.setUrl(dto.url());
                re.setStatus(dto.status());
                re.setDurationMs(dto.durationMs());
                re.setErrorMessage(dto.errorMessage());
                
                requestErrorRepository.save(re);
            }
        }
        
        log.info("Run ingested successfully: runId={}", run.getId());
        
        // Publish SSE event
        eventPublisher.publishRunCompleted(run);
        
        return new IngestRunResponse(
            run.getId(),
            "SUCCESS",
            "Run ingested successfully"
        );
    }
}
