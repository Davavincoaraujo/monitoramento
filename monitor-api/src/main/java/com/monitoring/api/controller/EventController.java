package com.monitoring.api.controller;

import com.monitoring.api.service.EventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST Controller para Server-Sent Events (SSE) - Live Monitoring em tempo real.
 * 
 * <p>Permite que clientes estabeleçam conexão persistente para receber atualizações
 * em tempo real sobre execuções de um site.</p>
 * 
 * <p><b>Endpoint:</b></p>
 * <pre>
 * GET /api/events?siteId={id}  - Estabelece stream SSE para receber eventos do site
 * </pre>
 * 
 * <p><b>Protocolo SSE:</b></p>
 * <ul>
 *   <li>Content-Type: text/event-stream</li>
 *   <li>Connection: keep-alive persistente</li>
 *   <li>Eventos enviados como text/plain no formato SSE</li>
 *   <li>Cliente reconecta automaticamente em caso de timeout</li>
 * </ul>
 * 
 * <p><b>Eventos Publicados:</b></p>
 * <pre>
 * event: run-started
 * data: {"runId": 123, "siteId": 1, "startedAt": "2026-02-02T10:00:00"}
 * 
 * event: run-completed
 * data: {"runId": 123, "status": "SUCCESS", "criticalCount": 0, "majorCount": 0, "minorCount": 1}
 * 
 * event: run-failed
 * data: {"runId": 123, "error": "Timeout after 30s"}
 * </pre>
 * 
 * <p><b>Ciclo de Vida da Conexão:</b></p>
 * <ol>
 *   <li>Cliente faz GET /api/events?siteId=1</li>
 *   <li>EventPublisher cria SseEmitter e registra no mapa de subscribers</li>
 *   <li>EventPublisher envia eventos quando runs são ingeridas</li>
 *   <li>Conexão persiste até: timeout (30min), erro de rede, cliente desconectar</li>
 *   <li>onTimeout/onError: subscriber é removido do mapa</li>
 * </ol>
 * 
 * <p><b>Configuração de Timeout:</b></p>
 * <pre>
 * SseEmitter timeout = 30 minutos (1_800_000 ms)
 * Cliente deve reconectar após timeout
 * </pre>
 * 
 * <p><b>Uso no Frontend (JavaScript):</b></p>
 * <pre>
 * const eventSource = new EventSource('/api/events?siteId=1');
 * 
 * eventSource.addEventListener('run-completed', (e) => {
 *   const data = JSON.parse(e.data);
 *   console.log('Run completed:', data);
 *   updateDashboard(data);
 * });
 * 
 * eventSource.onerror = (err) => {
 *   console.error('SSE error:', err);
 *   eventSource.close();  // Reconectar após delay
 * };
 * </pre>
 * 
 * <p><b>Escalabilidade:</b></p>
 * <ul>
 *   <li>Cada cliente mantém 1 conexão HTTP persistente</li>
 *   <li>EventPublisher gerencia mapa ConcurrentHashMap de emitters</li>
 *   <li>Broadcast para múltiplos clientes do mesmo siteId</li>
 *   <li>Limpeza automática de conexões mortas (timeout/error handlers)</li>
 *   <li>Para clusters: considerar Redis Pub/Sub ou WebSockets</li>
 * </ul>
 * 
 * <p><b>Alternativas:</b></p>
 * <ul>
 *   <li>WebSockets: full-duplex, mais complexo, overhead maior</li>
 *   <li>Long Polling: menos eficiente, mais latência</li>
 *   <li>SSE: unidirecional, simples, built-in browser, ideal para monitoring</li>
 * </ul>
 * 
 * <p><b>Tratamento de Erros:</b></p>
 * <ul>
 *   <li>400 Bad Request: siteId ausente ou inválido</li>
 *   <li>500 Internal Error: falha ao criar SseEmitter</li>
 *   <li>Timeout: cliente deve reconectar automaticamente</li>
 * </ul>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see EventPublisher
 * @see SseEmitter
 * @see org.springframework.web.servlet.mvc.method.annotation.SseEmitter
 */
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
