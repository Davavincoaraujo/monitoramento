package com.monitoring.api.scheduler;

import com.monitoring.api.service.CheckSchedulerService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * Quartz Job que executa agendamento periódico de checks de sites.
 * 
 * <p>Este job é disparado pelo Quartz Scheduler a cada minuto (cron: 0 * * * * ?)
 * e delega para CheckSchedulerService a lógica de determinar quais sites devem
 * ser verificados.</p>
 * 
 * <p><b>Configuração do Job (QuartzConfig):</b></p>
 * <pre>
 * Trigger: CronTrigger
 * Cron: 0 * * * * ?  (a cada minuto, no segundo 0)
 * Job: CheckSchedulerJob
 * Job Store: RAM (in-memory, não persiste)
 * </pre>
 * 
 * <p><b>Fluxo de Execução:</b></p>
 * <ol>
 *   <li>Quartz dispara execute() a cada minuto</li>
 *   <li>CheckSchedulerService.scheduleChecks() é chamado</li>
 *   <li>Service consulta sites habilitados no banco</li>
 *   <li>Verifica última run de cada site</li>
 *   <li>Se frequencySeconds expirou, publica mensagem RabbitMQ</li>
 * </ol>
 * 
 * <p><b>Design Pattern:</b></p>
 * <pre>
 * - Job (Quartz): responsável apenas por trigger temporal
 * - Service: contém lógica de negócio (quais sites agendar)
 * - Separation of Concerns: scheduling vs business logic
 * </pre>
 * 
 * <p><b>Injeção de Dependência:</b></p>
 * <ul>
 *   <li>Quartz integra com Spring via AutowiringSpringBeanJobFactory</li>
 *   <li>CheckSchedulerService injetado via construtor</li>
 *   <li>Job é @Component para ser gerenciado pelo Spring</li>
 * </ul>
 * 
 * <p><b>Tratamento de Erros:</b></p>
 * <pre>
 * - Se CheckSchedulerService.scheduleChecks() lançar exceção:
 *   * Quartz registra erro no log
 *   * Próxima execução ocorre normalmente (não interrompe scheduler)
 *   * JobExecutionException pode configurar retry/refire
 * </pre>
 * 
 * <p><b>Performance:</b></p>
 * <ul>
 *   <li>Execução deve ser rápida (< 1s idealmente)</li>
 *   <li>Não bloqueia: apenas publica mensagens assíncronas</li>
 *   <li>Checks reais executam no monitor-runner (desacoplado)</li>
 *   <li>Se execução demorar > 1min, próxima execução espera finalizar</li>
 * </ul>
 * 
 * <p><b>Escalabilidade:</b></p>
 * <ul>
 *   <li>Job Store RAM: não compartilha entre instâncias</li>
 *   <li>Para clusters: usar JDBC Job Store + lock pessimista</li>
 *   <li>Ou: executar scheduler em apenas 1 instância (@Profile)</li>
 * </ul>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 * @see CheckSchedulerService
 * @see org.quartz.Job
 * @see com.monitoring.api.config.QuartzConfig
 */
@Component
public class CheckSchedulerJob implements Job {
    
    private final CheckSchedulerService checkSchedulerService;
    
    public CheckSchedulerJob(CheckSchedulerService checkSchedulerService) {
        this.checkSchedulerService = checkSchedulerService;
    }
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        checkSchedulerService.scheduleChecks();
    }
}
