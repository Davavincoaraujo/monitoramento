# Site Monitoring System

Sistema completo de monitoramento sintético de sites com testes automatizados usando Playwright, dashboard em tempo real via SSE e relatórios semanais por e-mail.

## 🏗️ Arquitetura

Monorepo com 2 aplicações Spring Boot:

- **monitor-api** (porta 8080): REST API, ingest, SSE, Quartz scheduler, dashboard
- **monitor-runner** (porta 8081): Playwright executor, RabbitMQ consumer

### Stack Tecnológica

- Java 21
- Spring Boot 3.2.2
- PostgreSQL 16 (Flyway migrations)
- RabbitMQ 3.12
- Playwright Java 1.41
- Quartz Scheduler
- Thymeleaf (email templates)

## 🚀 Quick Start

### 1. Pré-requisitos

```bash
# Java 21
java -version

# Maven
mvn -version

# Docker & Docker Compose
docker --version
docker-compose --version
```

### 2. Iniciar Infraestrutura

```bash
# PostgreSQL + RabbitMQ + pgAdmin
docker-compose up -d

# Verificar status
docker-compose ps
```

**Serviços disponíveis:**
- PostgreSQL: `localhost:5432` (user: `monitor`, pass: `monitor123`, db: `monitoring`)
- RabbitMQ Management: http://localhost:15672 (user: `monitor`, pass: `monitor123`)
- pgAdmin: http://localhost:5050 (email: `admin@monitor.com`, pass: `admin123`)

### 3. Instalar Playwright Browsers

```bash
cd monitor-runner
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

### 4. Build e Run

**Terminal 1 - monitor-api:**
```bash
cd monitor-api
mvn clean install
mvn spring-boot:run
```

**Terminal 2 - monitor-runner:**
```bash
cd monitor-runner
mvn clean install
mvn spring-boot:run
```

### 5. Popular Dados de Teste (Opcional)

```sql
-- Conectar ao PostgreSQL
psql -h localhost -U monitor -d monitoring

-- Inserir site exemplo
INSERT INTO sites (name, base_url, enabled, frequency_seconds, email_recipients, created_at, updated_at)
VALUES ('Example Site', 'https://example.com', true, 300, 'admin@example.com', NOW(), NOW());

-- Inserir páginas
INSERT INTO site_pages (site_id, name, path, enabled, created_at)
VALUES 
    (1, 'Home', '/', true, NOW()),
    (1, 'About', '/about', true, NOW());
```

## 📡 API Endpoints

### Sites

```http
GET    /api/sites              # Listar todos os sites
GET    /api/sites/{id}         # Obter site específico
GET    /api/sites/{id}/config  # Config site + páginas (usado pelo runner)
```

### Dashboard

```http
GET /api/dashboard/overview
  ?siteId=1&range=24h
  # range: 1h, 6h, 24h, 7d, 30d

GET /api/dashboard/timeseries/errors
  ?siteId=1&range=24h&bucket=1h

GET /api/dashboard/timeseries/perf
  ?siteId=1&range=7d&bucket=1h
```

### Runs

```http
GET /api/runs?siteId=1&from=2024-01-01T00:00:00&to=2024-01-07T23:59:59
GET /api/runs/{id}
```

### Events (SSE)

```http
GET /api/events?siteId=1
# Server-Sent Events stream
# Events: connected, run_completed
```

### Ingest (interno - usado pelo runner)

```http
POST /api/ingest/runs
Content-Type: application/json

{
  "siteId": 1,
  "startedAt": "2024-01-01T10:00:00",
  "endedAt": "2024-01-01T10:02:30",
  "status": "SUCCESS",
  "summary": "Completed 2 pages",
  "pageResults": [...],
  "failures": [...],
  "requestErrors": [...]
}
```

## 🔄 Fluxo de Execução

1. **Quartz Job** (a cada minuto) identifica sites "due" (baseado em `frequency_seconds`)
2. Publica mensagem `RUN_CHECK` no RabbitMQ com `siteId`
3. **monitor-runner** consome mensagem
4. Busca configuração via `GET /api/sites/{id}/config`
5. Executa Playwright para cada página:
   - Intercepta requests/responses
   - Captura console errors e JS errors
   - Coleta métricas (TTFB, DOM, Load)
   - Detecta assets 404, XHR 5xx, etc.
6. Envia resultados via `POST /api/ingest/runs`
7. **monitor-api** persiste no banco e emite evento SSE

## 📊 Relatório Semanal

- **Agendamento**: Domingos 20:00 (America/Sao_Paulo) via Quartz
- **Conteúdo**:
  - Uptime % da semana
  - Contagem de falhas por severidade
  - P95 Load e TTFB (comparado com semana anterior)
  - Top 5 problemas recorrentes
  - Top 5 páginas mais lentas
  - Top 10 assets 404
- **Envio**: SMTP ou Fake (dev)

## 🔧 Configuração

### monitor-api/src/main/resources/application.yml

```yaml
monitoring:
  default-check-frequency-seconds: 300
  max-concurrent-runs: 5
  data-retention-days: 30
  weekly-report-timezone: America/Sao_Paulo
  weekly-report-cron: "0 0 20 ? * SUN"
  email:
    from: noreply@monitoring.com
    enabled: false  # true para SMTP real

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
```

### monitor-runner/src/main/resources/application.yml

```yaml
playwright:
  headless: true
  timeout-ms: 30000
  viewport-width: 1920
  viewport-height: 1080

monitor:
  api:
    base-url: http://localhost:8080
```

## 📐 Schema Resumido

```sql
sites              # Sites monitorados
site_pages         # Páginas por site
rules              # Regras configuráveis (futuro)
runs               # Execuções com status e contadores
page_results       # Métricas de performance por página
failures           # Falhas agregadas (CRITICAL/MAJOR/MINOR)
request_errors     # Erros detalhados de requests (retenção 30d)
```

## 🎯 Severidade Automática

- **CRITICAL**: JS/CSS 404, XHR/fetch 5xx, navigation failed, page crash
- **MAJOR**: IMG 404, console error, thresholds excedidos
- **MINOR**: warnings, ocorrências isoladas

## 🧪 Testando SSE

```bash
curl -N http://localhost:8080/api/events?siteId=1
```

Ou via navegador:
```javascript
const eventSource = new EventSource('http://localhost:8080/api/events?siteId=1');
eventSource.addEventListener('run_completed', (event) => {
  console.log('Run completed:', JSON.parse(event.data));
});
```

## 🐛 Troubleshooting

### Playwright não encontra chromium
```bash
cd monitor-runner
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

### RabbitMQ connection refused
```bash
docker-compose ps
# Se stopped, iniciar:
docker-compose up -d rabbitmq
```

### Flyway migration fail
```bash
# Dropar banco e recriar
docker-compose down -v
docker-compose up -d postgres
# Aguardar 10s, então iniciar monitor-api
```

## 📈 Melhorias Futuras

- [ ] WebSocket como alternativa ao SSE
- [ ] Dashboard frontend (React/Vue)
- [ ] Alertas em tempo real (Slack, PagerDuty)
- [ ] Métricas do runner (Prometheus/Grafana)
- [ ] Particionamento de tabelas por mês
- [ ] Compressão de screenshots on failure
- [ ] Suporte a autenticação (OAuth2, Basic)
- [ ] Rate limiting no ingest
- [ ] Retry policy no runner com backoff exponencial
- [ ] Health checks e readiness probes (K8s)

## 📝 Licença

MIT

## 👨‍💻 Autor

Sistema desenvolvido para monitoramento sintético escalável de sites e aplicações web.
