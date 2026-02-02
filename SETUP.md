# SETUP.md

## Guia Completo de Setup

### 1. Instalar Dependências

```bash
# Verificar Java 21
java -version
# java version "21.0.x"

# Verificar Maven
mvn -version
# Apache Maven 3.9.x

# Verificar Docker
docker --version
docker-compose --version
```

### 2. Iniciar Infraestrutura (PostgreSQL + RabbitMQ)

```bash
# Na raiz do projeto
docker-compose up -d

# Aguardar 10-15 segundos para inicialização completa
docker-compose ps

# Verificar logs se necessário
docker-compose logs postgres
docker-compose logs rabbitmq
```

### 3. Instalar Playwright Browsers

```bash
cd monitor-runner

# Instalar apenas Chromium (mais leve)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# OU instalar todos os browsers (dev/teste)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

cd ..
```

### 4. Build e Run monitor-api

```bash
# Terminal 1
cd monitor-api

# Compilar
mvn clean install

# Executar (porta 8080)
mvn spring-boot:run

# Aguardar mensagem: "Started MonitorApiApplication in X seconds"
```

**Flyway vai criar automaticamente as tabelas no PostgreSQL na primeira execução.**

### 5. Popular Dados de Exemplo (Opcional)

```bash
# Em outro terminal
psql -h localhost -U monitor -d monitoring -f seed-data.sql

# Senha quando solicitado: monitor123
```

### 6. Build e Run monitor-runner

```bash
# Terminal 2
cd monitor-runner

# Compilar
mvn clean install

# Executar (porta 8081)
mvn spring-boot:run

# Aguardar mensagem: "Started MonitorRunnerApplication in X seconds"
```

### 7. Verificar Sistema Funcionando

#### a) Verificar APIs

```bash
# Listar sites
curl http://localhost:8080/api/sites

# Obter configuração de um site
curl http://localhost:8080/api/sites/1/config

# Dashboard overview
curl "http://localhost:8080/api/dashboard/overview?siteId=1&range=24h"
```

#### b) Verificar RabbitMQ

Acessar: http://localhost:15672
- User: `monitor`
- Pass: `monitor123`

Ir em **Queues** e verificar `monitor.run-check`.

#### c) Verificar PostgreSQL (pgAdmin)

Acessar: http://localhost:5050
- Email: `admin@monitor.com`  
- Pass: `admin123`

**Adicionar servidor:**
- Host: `postgres` (nome do container)
- Port: `5432`
- Database: `monitoring`
- Username: `monitor`
- Password: `monitor123`

#### d) Monitorar Logs

```bash
# Terminal 3 - Logs do monitor-api
cd monitor-api
tail -f target/*.log

# Terminal 4 - Logs do monitor-runner  
cd monitor-runner
tail -f target/*.log
```

### 8. Trigger Manual de Check

O Quartz Job roda a cada minuto e publica checks para sites que não rodaram há X segundos (baseado em `frequency_seconds`).

Para forçar um teste imediato, você pode publicar manualmente no RabbitMQ:

```bash
# Via RabbitMQ Management UI
# http://localhost:15672 > Queues > monitor.run-check > Publish Message

# Payload exemplo:
{
  "siteId": 1,
  "siteName": "Google",
  "baseUrl": "https://www.google.com"
}
```

Ou criar um endpoint temporário no monitor-api:

```java
@PostMapping("/api/debug/trigger-check/{siteId}")
public ResponseEntity<String> triggerCheck(@PathVariable Long siteId) {
    Site site = siteRepository.findById(siteId).orElseThrow();
    RunCheckMessage message = new RunCheckMessage(
        site.getId(), site.getName(), site.getBaseUrl()
    );
    rabbitTemplate.convertAndSend(RabbitMQConfig.RUN_CHECK_QUEUE, message);
    return ResponseEntity.ok("Check triggered");
}
```

### 9. Testar SSE (Server-Sent Events)

```bash
# Terminal
curl -N http://localhost:8080/api/events?siteId=1

# Deve imprimir:
# event: connected
# data: Connected to monitoring events

# Aguardar um check completar para ver:
# event: run_completed
# data: {"runId":1,"status":"SUCCESS",...}
```

Ou via browser console:
```javascript
const es = new EventSource('http://localhost:8080/api/events?siteId=1');
es.addEventListener('run_completed', e => console.log(JSON.parse(e.data)));
```

### 10. Verificar Weekly Report Job

O job de relatório semanal roda domingos 20:00 (America/Sao_Paulo).

Para testar manualmente sem aguardar:

```bash
# Acessar banco e verificar Quartz
psql -h localhost -U monitor -d monitoring

SELECT * FROM qrtz_triggers WHERE trigger_name = 'weeklyReportTrigger';

# Para disparar manualmente, criar endpoint temporário:
```

```java
@PostMapping("/api/debug/trigger-report/{siteId}")
public ResponseEntity<String> triggerReport(@PathVariable Long siteId) {
    Site site = siteRepository.findById(siteId).orElseThrow();
    weeklyReportService.generateAndSendReport(site); // criar método público
    return ResponseEntity.ok("Report generated");
}
```

### 11. Configurar SMTP Real (Produção)

Editar `monitor-api/src/main/resources/application.yml`:

```yaml
monitoring:
  email:
    enabled: true

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
```

Para Gmail, gerar App Password: https://myaccount.google.com/apppasswords

### 12. Troubleshooting Comum

#### Erro: "Playwright browser not found"
```bash
cd monitor-runner
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

#### Erro: "Connection refused postgres"
```bash
docker-compose up -d postgres
# Aguardar 10 segundos
docker-compose logs postgres | grep "ready to accept connections"
```

#### Erro: "Queue not found" no runner
```bash
# Verificar se API criou a queue
curl -u monitor:monitor123 http://localhost:15672/api/queues/%2F/monitor.run-check

# Se não existir, reiniciar monitor-api
```

#### Flyway erro "Validate failed"
```bash
# Limpar tudo e recriar
docker-compose down -v
docker-compose up -d
# Aguardar 15s
cd monitor-api && mvn spring-boot:run
```

### 13. Parar Ambiente

```bash
# Parar aplicações
# Ctrl+C nos terminais do Spring Boot

# Parar infraestrutura
docker-compose down

# Parar e remover volumes (apaga dados)
docker-compose down -v
```

### 14. Rebuild Após Mudanças

```bash
# monitor-api
cd monitor-api
mvn clean install
mvn spring-boot:run

# monitor-runner
cd monitor-runner
mvn clean install
mvn spring-boot:run
```

### 15. Executar Testes (quando implementados)

```bash
cd monitor-api
mvn test

cd monitor-runner
mvn test
```

## Arquitetura de Deployment

### Desenvolvimento
- 2 terminais Spring Boot (local)
- Docker Compose (infra local)

### Produção (sugestão)
- Kubernetes/ECS com 2+ replicas do monitor-runner
- PostgreSQL RDS/Cloud SQL
- RabbitMQ CloudAMQP/Amazon MQ
- Load Balancer na frente do monitor-api
- Redis para SSE (quando escalar além de 1 instância API)

## Monitoramento do Sistema

### Métricas Importantes

- Taxa de sucesso dos checks (%)
- Latência p95 do ingest
- Tamanho da fila RabbitMQ
- Concurrent runs no runner
- Database connection pool utilization

### Logs

```bash
# Buscar erros
grep ERROR monitor-api/target/*.log
grep ERROR monitor-runner/target/*.log

# Buscar checks completados
grep "Check completed" monitor-runner/target/*.log

# Buscar ingest
grep "Ingested run" monitor-api/target/*.log
```

## Performance Tips

1. **Ajustar pool de threads no runner** (`application.yml`):
   ```yaml
   spring.rabbitmq.listener.simple:
     concurrency: 3
     max-concurrency: 10
   ```

2. **Tuning PostgreSQL connection pool** (API):
   ```yaml
   spring.datasource.hikari:
     maximum-pool-size: 30
     minimum-idle: 10
   ```

3. **Playwright timeout ajustes**:
   - `timeout-ms: 20000` para sites rápidos
   - `timeout-ms: 60000` para sites lentos

4. **Batch inserts** já habilitado:
   ```yaml
   spring.jpa.properties.hibernate:
     jdbc.batch_size: 20
     order_inserts: true
   ```

## Próximos Passos

1. Criar frontend dashboard (React/Vue)
2. Adicionar autenticação (Spring Security + JWT)
3. Implementar alertas (webhooks, Slack)
4. Adicionar health checks `/actuator/health`
5. Configurar Prometheus metrics
6. CI/CD pipeline (GitHub Actions)
