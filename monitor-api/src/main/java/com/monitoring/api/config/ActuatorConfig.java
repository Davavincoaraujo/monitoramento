package com.monitoring.api.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Configuração do Spring Actuator com health indicators customizados.
 * 
 * <p>Health checks são essenciais para Kubernetes:</p>
 * <ul>
 *   <li><b>Liveness</b>: Verifica se aplicação está rodando (JVM ativo, scheduler rodando)</li>
 *   <li><b>Readiness</b>: Verifica se aplicação está pronta para receber tráfego (DB, RabbitMQ OK)</li>
 * </ul>
 * 
 * <p><b>Endpoints Disponíveis:</b></p>
 * <pre>
 * GET /actuator/health        - Health geral (UP/DOWN)
 * GET /actuator/health/liveness  - Liveness probe (para K8s)
 * GET /actuator/health/readiness - Readiness probe (para K8s)
 * GET /actuator/metrics       - Métricas Micrometer
 * GET /actuator/info          - Informações da aplicação
 * </pre>
 * 
 * <p><b>Health Indicators Incluídos:</b></p>
 * <ul>
 *   <li>database: Verifica conexão PostgreSQL (Spring auto-config)</li>
 *   <li>rabbitmq: Verifica conexão RabbitMQ (Spring auto-config)</li>
 *   <li>quartz: Verifica se scheduler Quartz está ativo (custom)</li>
 *   <li>diskSpace: Verifica espaço em disco disponível (Spring auto-config)</li>
 * </ul>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 */
@Configuration
public class ActuatorConfig {
    
    /**
     * Health indicator customizado para verificar o Quartz Scheduler.
     * 
     * <p>Verifica se o scheduler está iniciado e rodando jobs.</p>
     * 
     * @param schedulerFactory Factory bean do Quartz
     * @return HealthIndicator que reporta UP se scheduler está rodando
     */
    @Bean
    public HealthIndicator quartzHealthIndicator(SchedulerFactoryBean schedulerFactory) {
        return () -> {
            try {
                var scheduler = schedulerFactory.getScheduler();
                
                if (scheduler.isStarted() && !scheduler.isShutdown()) {
                    int runningJobs = scheduler.getCurrentlyExecutingJobs().size();
                    return Health.up()
                        .withDetail("started", true)
                        .withDetail("runningJobs", runningJobs)
                        .withDetail("schedulerName", scheduler.getSchedulerName())
                        .build();
                } else {
                    return Health.down()
                        .withDetail("started", scheduler.isStarted())
                        .withDetail("shutdown", scheduler.isShutdown())
                        .build();
                }
            } catch (Exception e) {
                return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
            }
        };
    }
}
