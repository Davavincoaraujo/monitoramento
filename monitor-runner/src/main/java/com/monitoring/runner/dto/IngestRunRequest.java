package com.monitoring.runner.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IngestRunRequest {
    private Long siteId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String status;
    private String summary;
    private List<PageResultDTO> pageResults = new ArrayList<>();
    private List<FailureDTO> failures = new ArrayList<>();
    private List<RequestErrorDTO> requestErrors = new ArrayList<>();
    
    // Getters and setters
    public Long getSiteId() {
        return siteId;
    }
    
    public void setSiteId(Long siteId) {
        this.siteId = siteId;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public List<PageResultDTO> getPageResults() {
        return pageResults;
    }
    
    public void setPageResults(List<PageResultDTO> pageResults) {
        this.pageResults = pageResults;
    }
    
    public List<FailureDTO> getFailures() {
        return failures;
    }
    
    public void setFailures(List<FailureDTO> failures) {
        this.failures = failures;
    }
    
    public List<RequestErrorDTO> getRequestErrors() {
        return requestErrors;
    }
    
    public void setRequestErrors(List<RequestErrorDTO> requestErrors) {
        this.requestErrors = requestErrors;
    }
}
