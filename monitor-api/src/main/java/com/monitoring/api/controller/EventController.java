package com.monitoring.api.controller;

import com.monitoring.api.service.EventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class EventController {
    
    private final EventPublisher eventPublisher;
    
    public EventController(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    @GetMapping
    public SseEmitter streamEvents(@RequestParam Long siteId) {
        return eventPublisher.subscribe(siteId);
    }
}
