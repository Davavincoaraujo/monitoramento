package com.monitoring.runner.dto;

public class FailureDTO {
    private Long pageId;
    private String severity;
    private String type;
    private String message;
    private String url;
    
    public FailureDTO() {}
    
    public FailureDTO(Long pageId, String severity, String type, String message, String url) {
        this.pageId = pageId;
        this.severity = severity;
        this.type = type;
        this.message = message;
        this.url = url;
    }
    
    // Getters and setters
    public Long getPageId() {
        return pageId;
    }
    
    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
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
}
