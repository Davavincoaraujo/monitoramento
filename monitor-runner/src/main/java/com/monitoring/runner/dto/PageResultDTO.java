package com.monitoring.runner.dto;

public class PageResultDTO {
    private Long pageId;
    private String finalUrl;
    private Integer ttfbMs;
    private Integer domMs;
    private Integer loadMs;
    private Integer requestsCount;
    private Long totalBytes;
    
    // Getters and setters
    public Long getPageId() {
        return pageId;
    }
    
    public void setPageId(Long pageId) {
        this.pageId = pageId;
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
}
