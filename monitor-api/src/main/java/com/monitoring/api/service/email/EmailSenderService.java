package com.monitoring.api.service.email;

public interface EmailSenderService {
    void sendHtmlEmail(String to, String subject, String htmlBody);
}
