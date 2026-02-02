package com.monitoring.api.service;

import com.monitoring.api.config.RabbitMQConfig;
import com.monitoring.api.domain.entity.Site;
import com.monitoring.api.domain.repository.SiteRepository;
import com.monitoring.api.dto.message.RunCheckMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CheckSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(CheckSchedulerService.class);
    
    private final SiteRepository siteRepository;
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${monitoring.default-check-frequency-seconds:300}")
    private int defaultFrequencySeconds;
    
    public CheckSchedulerService(SiteRepository siteRepository, RabbitTemplate rabbitTemplate) {
        this.siteRepository = siteRepository;
        this.rabbitTemplate = rabbitTemplate;
    }
    
    public void scheduleChecks() {
        log.info("Running check scheduler");
        
        List<Site> enabledSites = siteRepository.findByEnabledTrue();
        
        for (Site site : enabledSites) {
            int frequencySeconds = site.getFrequencySeconds() != null 
                ? site.getFrequencySeconds() 
                : defaultFrequencySeconds;
            
            LocalDateTime cutoff = LocalDateTime.now().minusSeconds(frequencySeconds);
            
            List<Site> dueSites = siteRepository.findSitesDueForCheck(cutoff);
            
            if (dueSites.stream().anyMatch(s -> s.getId().equals(site.getId()))) {
                publishRunCheck(site);
            }
        }
    }
    
    private void publishRunCheck(Site site) {
        RunCheckMessage message = new RunCheckMessage(
            site.getId(),
            site.getName(),
            site.getBaseUrl()
        );
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.RUN_CHECK_QUEUE, message);
        log.info("Published run check for site: {} (id={})", site.getName(), site.getId());
    }
}
