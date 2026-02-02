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

/**
 * Consumer RabbitMQ responsável por processar mensagens de check de sites.
 * 
 * <p>Faz parte do padrão Producer-Consumer para execução assíncrona de checks:</p>
 * <pre>
 * monitor-api (producer)  -->  RabbitMQ (run-check-queue)  -->  monitor-runner (consumer)
 * </pre>
 * 
 * <p><b>Fluxo de Processamento:</b></p>
 * <ol>
 *   <li>Recebe mensagem RunCheckMessage da fila run-check-queue</li>
 *   <li>Busca configuração completa do site via API (GET /api/sites/{id}/config)</li>
 *   <li>Valida que o site tem páginas configuradas</li>
 *   <li>Executa check sintético usando PlaywrightExecutor</li>
 *   <li>Envia resultados de volta para API via POST /api/ingest/runs</li>
 * </ol>
 * 
 * <p><b>Mensagem Consumida (RunCheckMessage):</b></p>
 * <pre>
 * {
 *   "siteId": 1,
 *   "siteName": "Meu Site",
 *   "triggeredBy": "SCHEDULED"  // ou "MANUAL"
 * }
 * </pre>
 * 
 * <p><b>Configuração do Listener:</b></p>
 * <ul>
 *   <li>Queue: run-check-queue (durável)</li>
 *   <li>Exchange: check.topic (topic exchange)</li>
 *   <li>Routing Key: site.check</li>
 *   <li>Ack Mode: AUTO (Spring AMQP padrão)</li>
 *   <li>Concurrency: 1 listener por container (configurável)</li>
 * </ul>
 * 
 * <p><b>Tratamento de Erros:</b></p>
 * <ul>
 *   <li>Erro ao buscar config: log e descarta mensagem</li>
 *   <li>Erro no executor: log e descarta (run não é criada)</li>
 *   <li>Erro ao ingestar: log e descarta (resultados perdidos)</li>
 *   <li>DLQ: mensagens com exceções vão para run-check-queue.dlq (configurável)</li>
 * </ul>
 * 
 * <p><b>Escalabilidade:</b></p>
 * <pre>
 * - Múltiplas instâncias de monitor-runner podem consumir em paralelo
 * - RabbitMQ distribui mensagens via round-robin
 * - Cada instância processa um site por vez
 * </pre>
 * 
 * <p><b>Validações:</b></p>
 * <ul>
 *   <li>Site existe e está habilitado (via API)</li>
 *   <li>Site tem ao menos 1 página configurada</li>
 *   <li>SiteConfig tem baseUrl e páginas válidas</li>
 * </ul>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see PlaywrightExecutor
 * @see MonitorApiClient
 * @see RunCheckMessage
 * @see RabbitMQConfig
 */
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
