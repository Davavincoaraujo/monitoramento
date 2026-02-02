package com.monitoring.runner.client;

import com.monitoring.runner.dto.IngestRunRequest;
import com.monitoring.runner.dto.SiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class MonitorApiClient {
    private static final Logger log = LoggerFactory.getLogger(MonitorApiClient.class);
    
    private final RestTemplate restTemplate;
    private final String apiBaseUrl;
    
    public MonitorApiClient(
            RestTemplateBuilder builder,
            @Value("${monitor.api.base-url}") String apiBaseUrl,
            @Value("${monitor.api.connect-timeout:10000}") long connectTimeout,
            @Value("${monitor.api.read-timeout:30000}") long readTimeout) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(connectTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .build();
        this.apiBaseUrl = apiBaseUrl;
    }
    
    public SiteConfig getSiteConfig(Long siteId) {
        String url = apiBaseUrl + "/api/sites/" + siteId + "/config";
        
        try {
            ResponseEntity<SiteConfig> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                SiteConfig.class
            );
            
            log.info("Fetched site config for siteId={}", siteId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch site config for siteId={}", siteId, e);
            throw new RuntimeException("Failed to fetch site config", e);
        }
    }
    
    public void sendIngestRun(IngestRunRequest request) {
        String url = apiBaseUrl + "/api/ingest/runs";
        
        try {
            HttpEntity<IngestRunRequest> entity = new HttpEntity<>(request);
            restTemplate.postForEntity(url, entity, Object.class);
            
            log.info("Ingested run for siteId={}", request.getSiteId());
        } catch (Exception e) {
            log.error("Failed to ingest run for siteId={}", request.getSiteId(), e);
            throw new RuntimeException("Failed to ingest run", e);
        }
    }
}
