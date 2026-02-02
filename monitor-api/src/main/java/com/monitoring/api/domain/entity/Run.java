package com.monitoring.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.monitoring.api.domain.enums.RunStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa uma execução (Run) de check em um Site.
 * 
 * <p>Uma Run é criada cada vez que o sistema executa um check sintético em um site.
 * Ela agrega:</p>
 * <ul>
 *   <li>Resultados de performance por página (PageResult)</li>
 *   <li>Falhas detectadas (Failure)</li>
 *   <li>Erros de request HTTP (RequestError)</li>
 *   <li>Contadores de issues por severidade</li>
 *   <li>Status final da execução</li>
 *   <li>Timestamps de início e fim</li>
 * </ul>
 * 
 * <p><b>Status possíveis (RunStatus):</b></p>
 * <pre>
 * RUNNING  - Execução em andamento
 * SUCCESS  - Concluído sem issues críticas/major (apenas minor ou nenhuma)
 * WARNING  - Concluído apenas com issues minor
 * FAILED   - Concluído com issues críticas ou major
 * ERROR    - Falha na própria execução (timeout, crash, etc)
 * </pre>
 * 
 * <p><b>Contadores de Severidade:</b></p>
 * <ul>
 *   <li>criticalCount: Issues de severidade CRITICAL (impacto alto)</li>
 *   <li>majorCount: Issues de severidade MAJOR (impacto médio)</li>
 *   <li>minorCount: Issues de severidade MINOR (impacto baixo)</li>
 * </ul>
 * 
 * <p><b>Relacionamentos:</b></p>
 * <pre>
 * Run N:1 Site            - Cada run pertence a um site
 * Run 1:N PageResult      - Uma run testa múltiplas páginas
 * Run 1:N Failure         - Uma run pode ter múltiplas falhas
 * Run 1:N RequestError    - Uma run pode ter múltiplos erros HTTP
 * </pre>
 * 
 * <p><b>Lifecycle:</b></p>
 * <ol>
 *   <li>Criação: status=RUNNING, startedAt=now()</li>
 *   <li>Execução: playwright coleta métricas, detecta erros</li>
 *   <li>Finalização: endedAt=now(), status atualizado baseado em contadores</li>
 *   <li>Persistência: cascade ALL para resultados, falhas, erros</li>
 * </ol>
 * 
 * <p><b>Queries Otimizadas:</b></p>
 * <pre>
 * - Index composto em (site_id, started_at) para:
 *   * Buscar runs de um site ordenadas por data
 *   * Calcular uptime em período
 *   * Gerar séries temporais
 * </pre>
 * 
 * <p><b>Lazy Loading:</b></p>
 * <ul>
 *   <li>site: LAZY (evita N+1 queries)</li>
 *   <li>pageResults: LAZY (carrega sob demanda)</li>
 *   <li>failures: LAZY (pode ter muitas falhas)</li>
 *   <li>requestErrors: LAZY (pode ter muitos erros)</li>
 * </ul>
 * 
 * <p><b>IMPORTANTE:</b> Ao serializar para JSON, forçar hydrate das coleções lazy:
 * <pre>
 * run.getFailures().size();  // Força carregamento
 * </pre>
 * </p>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see Site
 * @see PageResult
 * @see Failure
 * @see RequestError
 * @see RunStatus
 */
@Entity
@Table(name = "runs", indexes = {
    @Index(name = "idx_runs_site_started", columnList = "site_id,started_at")
})
public class Run {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @JsonIgnoreProperties({"pages", "rules"})
    private Site site;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    @Column(name = "critical_count", nullable = false)
    private Integer criticalCount = 0;

    @Column(name = "major_count", nullable = false)
    private Integer majorCount = 0;

    @Column(name = "minor_count", nullable = false)
    private Integer minorCount = 0;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PageResult> pageResults = new ArrayList<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Failure> failures = new ArrayList<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestError> requestErrors = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Integer getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(Integer criticalCount) {
        this.criticalCount = criticalCount;
    }

    public Integer getMajorCount() {
        return majorCount;
    }

    public void setMajorCount(Integer majorCount) {
        this.majorCount = majorCount;
    }

    public Integer getMinorCount() {
        return minorCount;
    }

    public void setMinorCount(Integer minorCount) {
        this.minorCount = minorCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<PageResult> getPageResults() {
        return pageResults;
    }

    public void setPageResults(List<PageResult> pageResults) {
        this.pageResults = pageResults;
    }

    public List<Failure> getFailures() {
        return failures;
    }

    public void setFailures(List<Failure> failures) {
        this.failures = failures;
    }

    public List<RequestError> getRequestErrors() {
        return requestErrors;
    }

    public void setRequestErrors(List<RequestError> requestErrors) {
        this.requestErrors = requestErrors;
    }
}
