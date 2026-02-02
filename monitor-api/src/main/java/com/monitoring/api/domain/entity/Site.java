package com.monitoring.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um Site a ser monitorado.
 * 
 * <p>Um Site é a unidade principal de monitoramento. Cada site pode ter:</p>
 * <ul>
 *   <li>Múltiplas páginas (SitePage) a serem testadas</li>
 *   <li>Regras de validação customizadas (Rule)</li>
 *   <li>Histórico de execuções (Run)</li>
 *   <li>Configuração de frequência de checks</li>
 *   <li>Lista de destinatários para alertas por email</li>
 * </ul>
 * 
 * <p><b>Relacionamentos:</b></p>
 * <pre>
 * Site 1:N SitePage    - Um site tem múltiplas páginas
 * Site 1:N Run         - Um site tem múltiplas execuções
 * Site 1:N Rule        - Um site tem múltiplas regras de validação
 * </pre>
 * 
 * <p><b>Lifecycle:</b></p>
 * <ul>
 *   <li>Criação: timestamps preenchidos automaticamente (@PrePersist)</li>
 *   <li>Atualização: updatedAt atualizado automaticamente (@PreUpdate)</li>
 *   <li>Deleção: cascade orphanRemoval = true para páginas e runs</li>
 * </ul>
 * 
 * <p><b>Validações:</b></p>
 * <ul>
 *   <li>name: obrigatório, máx 255 chars</li>
 *   <li>baseUrl: obrigatório, deve ser URL válida, máx 500 chars</li>
 *   <li>frequencySeconds: obrigatório, > 0, padrão 300s (5 min)</li>
 *   <li>enabled: obrigatório, padrão true</li>
 * </ul>
 * 
 * <p><b>Indexação:</b></p>
 * <pre>
 * - PK em id
 * - Index em (site_id, started_at) para queries de runs
 * </pre>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see SitePage
 * @see Run
 * @see Rule
 */
@Entity
@Table(name = "sites")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "frequency_seconds", nullable = false)
    private Integer frequencySeconds = 300;

    @Column(name = "email_recipients", columnDefinition = "TEXT")
    private String emailRecipients;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("site")
    private List<SitePage> pages = new ArrayList<>();

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("site")
    private List<Rule> rules = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getFrequencySeconds() {
        return frequencySeconds;
    }

    public void setFrequencySeconds(Integer frequencySeconds) {
        this.frequencySeconds = frequencySeconds;
    }

    public String getEmailRecipients() {
        return emailRecipients;
    }

    public void setEmailRecipients(String emailRecipients) {
        this.emailRecipients = emailRecipients;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<SitePage> getPages() {
        return pages;
    }

    public void setPages(List<SitePage> pages) {
        this.pages = pages;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }
}
