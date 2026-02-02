package com.monitoring.runner.dto;

public class RequestErrorDTO {
    private String resourceType;
    private String url;
    private Integer status;
    private Integer durationMs;
    private String errorMessage;
    
    public RequestErrorDTO() {}
    
    public RequestErrorDTO(String resourceType, String url, Integer status, Integer durationMs, String errorMessage) {
        this.resourceType = resourceType;
        this.url = url;
        this.status = status;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
    }
    
    // Getters and setters
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Integer getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
