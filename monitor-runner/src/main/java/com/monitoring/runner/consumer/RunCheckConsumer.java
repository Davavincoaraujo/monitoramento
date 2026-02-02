package com.monitoring.runner.consumer;

import com.monitoring.runner.client.MonitorApiClient;
import com.monitoring.runner.config.RabbitMQConfig;
import com.monitoring.runner.dto.IngestRunRequest;
import com.monitoring.runner.dto.RunCheckMessage;
import com.monitoring.runner.dto.SiteConfig;
import com.monitoring.runner.playwright.PlaywrightExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RunCheckConsumer {
    private static final Logger log = LoggerFactory.getLogger(RunCheckConsumer.class);
    
    private final MonitorApiClient apiClient;
    private final PlaywrightExecutor executor;
    
    public RunCheckConsumer(MonitorApiClient apiClient, PlaywrightExecutor executor) {
        this.apiClient = apiClient;
        this.executor = executor;
    }
    
    @RabbitListener(queues = RabbitMQConfig.RUN_CHECK_QUEUE)
    public void handleRunCheck(RunCheckMessage message) {
        log.info("Received run check message for site: {} (id={})", 
            message.siteName(), message.siteId());
        
        try {
            // Fetch site configuration
            SiteConfig siteConfig = apiClient.getSiteConfig(message.siteId());
            
            if (siteConfig.pages() == null || siteConfig.pages().isEmpty()) {
                log.warn("No pages configured for site: {}", message.siteName());
                return;
            }
            
            // Execute check
            IngestRunRequest runResult = executor.executeCheck(siteConfig);
            
            // Send results back to API
            apiClient.sendIngestRun(runResult);
            
            log.info("Completed and ingested run for site: {}", message.siteName());
        } catch (Exception e) {
            log.error("Failed to process run check for site: {}", message.siteName(), e);
        }
    }
}
