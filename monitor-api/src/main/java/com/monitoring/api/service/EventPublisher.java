package com.monitoring.api.service;

import com.monitoring.api.domain.entity.Run;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private static final long SSE_TIMEOUT = 3600000L; // 1 hour
    
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> siteEmitters = new ConcurrentHashMap<>();
    
    public SseEmitter subscribe(Long siteId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        siteEmitters.computeIfAbsent(siteId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> removeEmitter(siteId, emitter));
        emitter.onTimeout(() -> removeEmitter(siteId, emitter));
        emitter.onError(e -> removeEmitter(siteId, emitter));
        
        log.info("New SSE subscriber for siteId={}", siteId);
        
        // Send initial heartbeat
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected to monitoring events"));
        } catch (IOException e) {
            removeEmitter(siteId, emitter);
        }
        
        return emitter;
    }
    
    public void publishRunCompleted(Run run) {
        Long siteId = run.getSite().getId();
        CopyOnWriteArrayList<SseEmitter> emitters = siteEmitters.get(siteId);
        
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        
        String eventData = String.format(
            "{\"runId\":%d,\"status\":\"%s\",\"critical\":%d,\"major\":%d,\"minor\":%d}",
            run.getId(),
            run.getStatus(),
            run.getCriticalCount(),
            run.getMajorCount(),
            run.getMinorCount()
        );
        
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("run_completed")
                    .data(eventData));
                return false;
            } catch (IOException e) {
                log.warn("Failed to send SSE event to client", e);
                return true;
            }
        });
        
        log.info("Published run_completed event for runId={} to {} clients", run.getId(), emitters.size());
    }
    
    private void removeEmitter(Long siteId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = siteEmitters.get(siteId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                siteEmitters.remove(siteId);
            }
        }
    }
}
