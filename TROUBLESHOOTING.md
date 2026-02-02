# 🔧 Troubleshooting Guide

## Quick Diagnostics

### Verificar Status dos Serviços

```bash
# Docker services
docker-compose ps

# Deve mostrar 3 containers running:
# - monitor-postgres
# - monitor-rabbitmq
# - monitor-pgadmin

# Se algum estiver stopped:
docker-compose up -d
```

### Verificar Aplicações

```bash
# monitor-api health (porta 8080)
curl http://localhost:8080/api/sites

# monitor-runner health (porta 8081 - sem endpoints expostos)
# Verificar logs

# RabbitMQ Management
curl -u monitor:monitor123 http://localhost:15672/api/overview

# PostgreSQL connection
psql -h localhost -U monitor -d monitoring -c "SELECT COUNT(*) FROM sites;"
```

---

## Problemas Comuns e Soluções

### ❌ Erro: "Playwright browser not found"

**Sintoma:**
```
Error: Executable doesn't exist at /Users/.../chromium-1097/chrome-mac/Chromium.app
```

**Solução:**
```bash
cd monitor-runner
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

Se persistir:
```bash
# Instalar todos os browsers
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

---

### ❌ Erro: "Connection refused" ao PostgreSQL

**Sintoma:**
```
Connection refused: localhost:5432
```

**Diagnóstico:**
```bash
docker-compose ps postgres
docker-compose logs postgres
```

**Solução:**
```bash
# Restartar PostgreSQL
docker-compose restart postgres

# Aguardar 10-15 segundos
sleep 15

# Verificar se está ready
docker-compose logs postgres | grep "ready to accept connections"

# Se ainda não funcionar, rebuild
docker-compose down
docker-compose up -d postgres
```

---

### ❌ Erro: "Queue not found" no monitor-runner

**Sintoma:**
```
Channel shutdown: channel error; protocol method: #method<channel.close>
(reply-code=404, reply-text=NOT_FOUND - no queue 'monitor.run-check')
```

**Causa:** monitor-api não criou a queue no RabbitMQ

**Solução:**
```bash
# 1. Verificar se monitor-api está rodando
curl http://localhost:8080/api/sites

# 2. Se não estiver, iniciar
cd monitor-api
mvn spring-boot:run

# 3. Aguardar boot completo (20-30s)

# 4. Verificar queue criada
curl -u monitor:monitor123 http://localhost:15672/api/queues/%2F/monitor.run-check

# 5. Se não existir, verificar logs
tail -f monitor-api/target/*.log | grep RabbitMQ

# 6. Reiniciar runner
cd monitor-runner
mvn spring-boot:run
```

---

### ❌ Flyway migration falha

**Sintoma:**
```
Validate failed: Migrations have failed validation
```

**Causa:** Schema já existe ou está corrompido

**Solução (Desenvolvimento):**
```bash
# CUIDADO: Apaga todos os dados
docker-compose down -v
docker-compose up -d postgres

# Aguardar 15 segundos
sleep 15

# Reiniciar monitor-api (Flyway vai recriar schema)
cd monitor-api
mvn spring-boot:run
```

**Solução (Produção):**
```bash
# Verificar versão atual
psql -h localhost -U monitor -d monitoring -c "SELECT * FROM flyway_schema_history;"

# Resolver manualmente ou aplicar nova migration
```

---

### ❌ Nenhum check está sendo executado

**Diagnóstico:**
```bash
# 1. Verificar se há sites cadastrados
curl http://localhost:8080/api/sites

# 2. Verificar Quartz jobs
psql -h localhost -U monitor -d monitoring -c "SELECT * FROM qrtz_triggers;"

# 3. Verificar logs do Quartz
tail -f monitor-api/target/*.log | grep Quartz

# 4. Verificar mensagens na fila RabbitMQ
# Acessar http://localhost:15672 > Queues > monitor.run-check
# Ver "Ready", "Unacked", "Total"
```

**Soluções:**

**a) Sem sites cadastrados:**
```bash
psql -h localhost -U monitor -d monitoring -f seed-data.sql
```

**b) Quartz não está rodando:**
```bash
# Verificar logs
tail -f monitor-api/target/*.log | grep CheckSchedulerJob

# Se não aparecer, verificar application.yml:
# spring.quartz.job-store-type: jdbc
```

**c) Sites não estão "due":**
```sql
-- Verificar último run de cada site
SELECT s.id, s.name, MAX(r.started_at) as last_run, s.frequency_seconds
FROM sites s
LEFT JOIN runs r ON r.site_id = s.id
WHERE s.enabled = true
GROUP BY s.id, s.name, s.frequency_seconds;
```

Se `last_run` é recente, ajustar frequency_seconds:
```sql
UPDATE sites SET frequency_seconds = 60 WHERE id = 1;
```

---

### ❌ Runner consome mensagem mas não executa check

**Diagnóstico:**
```bash
# Logs do runner
tail -f monitor-runner/target/*.log

# Verificar:
# 1. "Received run check message for site"
# 2. "Fetched site config"
# 3. "Starting check for site"
# 4. "Check completed"
# 5. "Completed and ingested run"
```

**Problemas comuns:**

**a) Erro ao buscar config (404):**
```bash
# Verificar endpoint manualmente
curl http://localhost:8080/api/sites/1/config

# Se falhar, verificar site existe
curl http://localhost:8080/api/sites/1
```

**b) Playwright timeout:**
```
TimeoutError: page.goto: Timeout 30000ms exceeded
```

Aumentar timeout em `monitor-runner/src/main/resources/application.yml`:
```yaml
playwright:
  timeout-ms: 60000  # 60 segundos
```

**c) Network error (site não acessível):**
```
net::ERR_NAME_NOT_RESOLVED
```

Verificar URL do site está correta:
```sql
SELECT id, name, base_url FROM sites;
```

---

### ❌ SSE não está funcionando

**Teste básico:**
```bash
# Terminal 1: Conectar SSE
curl -N http://localhost:8080/api/events?siteId=1

# Terminal 2: Trigger check manual
curl -X POST http://localhost:8080/api/debug/trigger-check/1
# (criar endpoint temporário ou publicar no RabbitMQ)

# Terminal 1 deve receber evento run_completed
```

**Se não funcionar:**
```bash
# Verificar logs
tail -f monitor-api/target/*.log | grep EventPublisher

# Verificar se IngestService chama EventPublisher
tail -f monitor-api/target/*.log | grep "Published run_completed"
```

---

### ❌ Email semanal não está sendo enviado

**Diagnóstico:**
```bash
# 1. Verificar job Quartz
psql -h localhost -U monitor -d monitoring -c "
  SELECT trigger_name, next_fire_time 
  FROM qrtz_triggers 
  WHERE trigger_name = 'weeklyReportTrigger';
"

# 2. Verificar logs
tail -f monitor-api/target/*.log | grep WeeklyReport

# 3. Verificar config email
grep -A 5 "monitoring.email" monitor-api/src/main/resources/application.yml
```

**Soluções:**

**a) Job não agendado:**
```bash
# Restartar monitor-api
cd monitor-api
mvn spring-boot:run
```

**b) Email fake (desenvolvimento):**
```yaml
# Em application.yml
monitoring:
  email:
    enabled: false  # false = FakeEmailSenderService (logs)
```

Para testar com fake:
```bash
# Logs mostram email "enviado"
tail -f monitor-api/target/*.log | grep "FAKE EMAIL"
```

**c) SMTP erro (produção):**
```
Mail server connection failed
```

Verificar credenciais em `application.yml`:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
```

Exportar variáveis de ambiente:
```bash
export SMTP_USERNAME=your-email@gmail.com
export SMTP_PASSWORD=your-app-password
```

---

### ❌ Performance degradada (muitos checks simultâneos)

**Diagnóstico:**
```bash
# Verificar conexões PostgreSQL
psql -h localhost -U monitor -d monitoring -c "
  SELECT COUNT(*) FROM pg_stat_activity 
  WHERE datname = 'monitoring';
"

# Verificar mensagens na fila RabbitMQ
# http://localhost:15672 > Queues > monitor.run-check
# Se "Ready" > 100, há backlog

# Verificar threads runner
tail -f monitor-runner/target/*.log | grep "Executing page"
```

**Soluções:**

**a) Limitar concorrência do runner:**
```yaml
# monitor-runner/src/main/resources/application.yml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 2        # Reduzir de 3 para 2
        max-concurrency: 3    # Reduzir de 5 para 3
```

**b) Aumentar connection pool:**
```yaml
# monitor-api/src/main/resources/application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30  # Aumentar de 20 para 30
```

**c) Reduzir frequência de checks:**
```sql
UPDATE sites SET frequency_seconds = 600 WHERE frequency_seconds < 600;
```

---

## Logs Úteis

### monitor-api
```bash
# Geral
tail -f monitor-api/target/*.log

# Apenas erros
tail -f monitor-api/target/*.log | grep ERROR

# Quartz jobs
tail -f monitor-api/target/*.log | grep "Quartz\|CheckScheduler\|WeeklyReport"

# Ingest
tail -f monitor-api/target/*.log | grep "Ingested run"

# SSE
tail -f monitor-api/target/*.log | grep "SSE\|EventPublisher"
```

### monitor-runner
```bash
# Geral
tail -f monitor-runner/target/*.log

# Checks
tail -f monitor-runner/target/*.log | grep "Starting check\|Check completed"

# Playwright errors
tail -f monitor-runner/target/*.log | grep "Playwright\|page.goto"

# RabbitMQ consumer
tail -f monitor-runner/target/*.log | grep "Received run check"
```

---

## Reset Completo (Dev)

Quando tudo falhar:

```bash
# 1. Parar tudo
# Ctrl+C em todos terminais Spring Boot

# 2. Limpar Docker (apaga TODOS os dados)
docker-compose down -v

# 3. Rebuild Maven
cd monitor-api && mvn clean install
cd ../monitor-runner && mvn clean install

# 4. Reinstalar Playwright
cd monitor-runner
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# 5. Iniciar infra
cd ..
docker-compose up -d

# 6. Aguardar 20 segundos
sleep 20

# 7. Iniciar monitor-api
cd monitor-api
mvn spring-boot:run
# Aguardar "Started MonitorApiApplication"

# 8. Popular dados
psql -h localhost -U monitor -d monitoring -f ../seed-data.sql

# 9. Iniciar monitor-runner
cd ../monitor-runner
mvn spring-boot:run
# Aguardar "Started MonitorRunnerApplication"

# 10. Verificar
curl http://localhost:8080/api/sites
```

---

## Checklist de Health Check

| Item | Comando | Esperado |
|------|---------|----------|
| PostgreSQL | `docker-compose ps postgres` | Up |
| RabbitMQ | `docker-compose ps rabbitmq` | Up |
| monitor-api | `curl http://localhost:8080/api/sites` | 200 OK |
| Queue criada | `curl -u monitor:monitor123 http://localhost:15672/api/queues` | monitor.run-check exists |
| Sites cadastrados | `curl http://localhost:8080/api/sites` | Array com sites |
| Quartz jobs | `psql -h localhost -U monitor -d monitoring -c "SELECT COUNT(*) FROM qrtz_triggers;"` | 2 |
| Playwright | `ls ~/.cache/ms-playwright/` | chromium-* dir |

---

## Contato de Suporte

1. Consultar README.md e SETUP.md primeiro
2. Verificar issues conhecidos neste documento
3. Verificar logs detalhados
4. Se o problema persistir, coletar:
   - Logs completos (monitor-api e monitor-runner)
   - `docker-compose logs`
   - Output de comandos de diagnóstico
   - Versões (Java, Maven, Docker)

---

**Última atualização:** 01/02/2026
