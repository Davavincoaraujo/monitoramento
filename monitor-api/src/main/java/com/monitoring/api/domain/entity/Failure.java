package com.monitoring.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.monitoring.api.domain.enums.FailureType;
import com.monitoring.api.domain.enums.Severity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade que representa uma Falha detectada durante a execução de um check.
 * 
 * <p>Uma Failure é criada quando o sistema detecta problemas durante o monitoramento:</p>
 * <ul>
 *   <li>Erros JavaScript na página</li>
 *   <li>Falhas de request HTTP (4xx, 5xx)</li>
 *   <li>Erros de console.error()</li>
 *   <li>Falhas de navegação (timeouts, crashes)</li>
 *   <li>Violações de regras customizadas</li>
 * </ul>
 * 
 * <p><b>Tipos de Failure (FailureType):</b></p>
 * <pre>
 * REQUEST_FAILED        - Request HTTP retornou status 4xx ou 5xx
 * JS_ERROR              - Erro JavaScript não-tratado (window.onerror)
 * CONSOLE_ERROR         - console.error() chamado no frontend
 * NAVIGATION_FAILED     - Falha ao navegar (timeout, crash)
 * RULE_VIOLATION        - Regra customizada violada
 * PERFORMANCE_DEGRADED  - Performance abaixo do threshold
 * </pre>
 * 
 * <p><b>Severidade (Severity):</b></p>
 * <pre>
 * CRITICAL - Impacto alto: site inacessível, erros fatais
 * MAJOR    - Impacto médio: features quebradas, erros importantes
 * MINOR    - Impacto baixo: warnings, performance degradada
 * </pre>
 * 
 * <p><b>Relacionamentos:</b></p>
 * <pre>
 * Failure N:1 Run       - Cada failure pertence a uma run
 * Failure N:1 SitePage  - Cada failure ocorre em uma página (opcional)
 * </pre>
 * 
 * <p><b>Campos:</b></p>
 * <ul>
 *   <li>severity: Nível de severidade (afeta contadores na Run)</li>
 *   <li>type: Tipo da falha (classificação)</li>
 *   <li>message: Mensagem descritiva do erro (TEXT, ilimitado)</li>
 *   <li>url: URL onde o erro ocorreu (máx 1000 chars)</li>
 *   <li>page: Página do site relacionada (nullable)</li>
 * </ul>
 * 
 * <p><b>Agregação:</b></p>
 * <pre>
 * Failures são contadas por severidade na entidade Run:
 * - run.criticalCount += 1 se severity = CRITICAL
 * - run.majorCount += 1 se severity = MAJOR
 * - run.minorCount += 1 se severity = MINOR
 * </pre>
 * 
 * <p><b>Queries Comuns:</b></p>
 * <pre>
 * // Top issues por severidade
 * SELECT type, COUNT(*) FROM failures
 * WHERE run_id IN (runs do período)
 * GROUP BY type, severity
 * ORDER BY severity DESC, COUNT(*) DESC
 * 
 * // Failures de uma página específica
 * SELECT * FROM failures
 * WHERE page_id = ? AND created_at > ?
 * ORDER BY severity DESC
 * </pre>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see Run
 * @see SitePage
 * @see FailureType
 * @see Severity
 */
@Entity
@Table(name = "failures")
public class Failure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    @JsonIgnoreProperties({"site", "pageResults", "failures", "requestErrors"})
    private Run run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    @JsonIgnoreProperties("site")
    private SitePage page;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FailureType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 1000)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Run getRun() {
        return run;
    }

    public void setRun(Run run) {
        this.run = run;
    }

    public SitePage getPage() {
        return page;
    }

    public void setPage(SitePage page) {
        this.page = page;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public FailureType getType() {
        return type;
    }

    public void setType(FailureType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
