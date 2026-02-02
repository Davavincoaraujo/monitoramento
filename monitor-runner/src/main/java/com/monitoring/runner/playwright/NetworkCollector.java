package com.monitoring.runner.playwright;

import com.microsoft.playwright.Response;
import com.monitoring.runner.dto.FailureDTO;
import com.monitoring.runner.dto.RequestErrorDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkCollector {
    private final List<RequestErrorDTO> requestErrors = new ArrayList<>();
    private final List<FailureDTO> failures = new ArrayList<>();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    
    public void onRequest() {
        requestCount.incrementAndGet();
    }
    
    public void onResponse(Response response) {
        try {
            int status = response.status();
            String url = response.url();
            String resourceType = response.request().resourceType();
            
            // Track response size
            if (response.body() != null) {
                totalBytes.addAndGet(response.body().length);
            }
            
            // Check for errors
            if (status >= 400) {
                RequestErrorDTO error = new RequestErrorDTO(
                    resourceType,
                    url,
                    status,
                    null,
                    response.statusText()
                );
                requestErrors.add(error);
                
                // Create failure based on status and resource type
                if (status >= 500) {
                    failures.add(new FailureDTO(
                        null,
                        "CRITICAL",
                        "XHR_5XX",
                        "Server error on " + resourceType + ": " + status,
                        url
                    ));
                } else if (status == 404) {
                    String failureType = getFailureTypeFor404(resourceType);
                    String severity = "stylesheet".equals(resourceType) || "script".equals(resourceType) 
                        ? "CRITICAL" : "MAJOR";
                    
                    failures.add(new FailureDTO(
                        null,
                        severity,
                        failureType,
                        "404 Not Found: " + resourceType,
                        url
                    ));
                }
            }
        } catch (Exception e) {
            // Ignore errors in collector
        }
    }
    
    public void onRequestFailed(String url, String errorMessage) {
        requestErrors.add(new RequestErrorDTO(
            "unknown",
            url,
            null,
            null,
            errorMessage
        ));
        
        failures.add(new FailureDTO(
            null,
            "CRITICAL",
            "REQUEST_FAILED",
            "Request failed: " + errorMessage,
            url
        ));
    }
    
    private String getFailureTypeFor404(String resourceType) {
        return switch (resourceType) {
            case "stylesheet" -> "CSS_404";
            case "script" -> "JS_404";
            case "image" -> "IMG_404";
            case "font" -> "FONT_404";
            default -> "ASSET_404";
        };
    }
    
    public List<RequestErrorDTO> getRequestErrors() {
        return requestErrors;
    }
    
    public List<FailureDTO> getFailures() {
        return failures;
    }
    
    public int getRequestCount() {
        return requestCount.get();
    }
    
    public long getTotalBytes() {
        return totalBytes.get();
    }
}
