package com.monitoring.runner.playwright;

import com.microsoft.playwright.ConsoleMessage;
import com.monitoring.runner.dto.FailureDTO;

import java.util.ArrayList;
import java.util.List;

public class ConsoleCollector {
    private final List<FailureDTO> failures = new ArrayList<>();
    
    public void onConsoleMessage(ConsoleMessage message) {
        String type = message.type();
        
        if ("error".equals(type) || "warning".equals(type)) {
            String severity = "error".equals(type) ? "MAJOR" : "MINOR";
            
            failures.add(new FailureDTO(
                null,
                severity,
                "CONSOLE_ERROR",
                "Console " + type + ": " + message.text(),
                null
            ));
        }
    }
    
    public void onPageError(String errorMessage) {
        failures.add(new FailureDTO(
            null,
            "CRITICAL",
            "JS_ERROR",
            "JavaScript error: " + errorMessage,
            null
        ));
    }
    
    public List<FailureDTO> getFailures() {
        return failures;
    }
}
