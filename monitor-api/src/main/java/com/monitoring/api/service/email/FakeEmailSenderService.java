package com.monitoring.api.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "monitoring.email.enabled", havingValue = "false", matchIfMissing = true)
public class FakeEmailSenderService implements EmailSenderService {
    private static final Logger log = LoggerFactory.getLogger(FakeEmailSenderService.class);
    
    @Override
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        log.info("=== FAKE EMAIL ===");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body length: {} chars", htmlBody.length());
        log.info("==================");
    }
}
