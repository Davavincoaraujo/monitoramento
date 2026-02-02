package com.monitoring.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "page_results")
public class PageResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    @JsonIgnoreProperties({"site", "pageResults", "failures", "requestErrors"})
    private Run run;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    @JsonIgnoreProperties("site")
    private SitePage page;

    @Column(name = "final_url", length = 1000)
    private String finalUrl;

    @Column(name = "ttfb_ms")
    private Integer ttfbMs;

    @Column(name = "dom_ms")
    private Integer domMs;

    @Column(name = "load_ms")
    private Integer loadMs;

    @Column(name = "requests_count", nullable = false)
    private Integer requestsCount = 0;

    @Column(name = "total_bytes", nullable = false)
    private Long totalBytes = 0L;

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

    public String getFinalUrl() {
        return finalUrl;
    }

    public void setFinalUrl(String finalUrl) {
        this.finalUrl = finalUrl;
    }

    public Integer getTtfbMs() {
        return ttfbMs;
    }

    public void setTtfbMs(Integer ttfbMs) {
        this.ttfbMs = ttfbMs;
    }

    public Integer getDomMs() {
        return domMs;
    }

    public void setDomMs(Integer domMs) {
        this.domMs = domMs;
    }

    public Integer getLoadMs() {
        return loadMs;
    }

    public void setLoadMs(Integer loadMs) {
        this.loadMs = loadMs;
    }

    public Integer getRequestsCount() {
        return requestsCount;
    }

    public void setRequestsCount(Integer requestsCount) {
        this.requestsCount = requestsCount;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
