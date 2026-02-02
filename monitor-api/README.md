# 🌐 Monitor API - Documentação

## Visão Geral

Módulo REST API responsável por:
- **Interface Web**: Dashboard e monitoramento ao vivo
- **Ingestão de Dados**: CRUD de sites e páginas
- **Agendamento**: Quartz scheduler para checks automáticos
- **Dashboard**: Endpoints de métricas e análise
- **Real-time**: Server-Sent Events (SSE) para feedback instantâneo
- **Producer**: Publicação de checks no RabbitMQ

**Porta:** 8080  
**Base URL:** http://localhost:8080

---

## 📁 Estrutura de Pastas

```
monitor-api/
├── src/main/java/com/monitoring/api/
│   ├── config/                    # Configurações da aplicação
│   │   ├── JacksonConfig.java    # Serialização JSON (Hibernate, JavaTime)
│   │   ├── QuartzConfig.java     # Configuração Quartz Scheduler
│   │   ├── RabbitMQConfig.java   # Exchanges, Queues, Bindings
│   │   └── WebConfig.java        # CORS, recursos estáticos
│   │
│   ├── controller/                # REST Controllers
│   │   ├── DashboardController.java  # Dashboard metrics
│   │   ├── LiveController.java       # SSE live monitoring
│   │   ├── RunController.java        # Runs CRUD
│   │   └── SiteController.java       # Sites CRUD + check trigger
│   │
│   ├── domain/                    # Domain layer
│   │   ├── entity/               # Entidades JPA
│   │   │   ├── Failure.java      # Falha detectada em run
│   │   │   ├── PageResult.java   # Resultado de uma página
│   │   │   ├── RequestError.java # Erro HTTP capturado
│   │   │   ├── Run.java          # Execução completa de check
│   │   │   ├── Site.java         # Site monitorado
│   │   │   └── SitePage.java     # Página dentro de um site
│   │   │
│   │   ├── repository/           # Spring Data JPA Repositories
│   │   │   ├── FailureRepository.java
│   │   │   ├── PageResultRepository.java
│   │   │   ├── RunRepository.java
│   │   │   ├── SitePageRepository.java
│   │   │   └── SiteRepository.java
│   │   │
│   │   └── enums/                # Enumerações
│   │       ├── FailureType.java  # REQUEST_FAILED, JS_ERROR, etc
│   │       ├── RunStatus.java    # RUNNING, SUCCESS, FAILED, etc
│   │       └── Severity.java     # CRITICAL, MAJOR, MINOR
│   │
│   ├── dto/                      # Data Transfer Objects
│   │   └── dashboard/            # DTOs específicos do dashboard
│   │       ├── DashboardOverviewDTO.java
│   │       ├── FailureDTO.java
│   │       ├── PerformanceMetricsDTO.java
│   │       ├── RunSummaryDTO.java
│   │       └── TimeSeriesDTO.java
│   │
│   ├── messaging/                # RabbitMQ Producer
│   │   ├── CheckProducer.java    # Publica checks no RabbitMQ
│   │   └── dto/
│   │       └── CheckRequestMessage.java
│   │
│   ├── scheduler/                # Quartz Jobs
│   │   ├── AutoCheckJob.java     # Job que executa checks a cada minuto
│   │   └── WeeklyReportScheduler.java  # Relatório semanal
│   │
│   ├── service/                  # Business Logic
│   │   ├── CheckService.java     # Orquestra checks
│   │   ├── DashboardService.java # Agregação de métricas
│   │   ├── EmailService.java     # Envio de emails
│   │   └── SiteService.java      # Lógica de sites
│   │
│   └── MonitorApiApplication.java  # Main class
│
└── src/main/resources/
    ├── application.properties    # Configuração principal
    │
    ├── static/                   # Arquivos estáticos (HTML, CSS, JS)
    │   ├── index.html           # Dashboard principal
    │   └── live.html            # Monitoramento ao vivo
    │
    ├── templates/                # Thymeleaf templates
    │   └── email/
    │       └── weekly-report.html  # Template email semanal
    │
    └── db/migration/             # Flyway migrations
        ├── V1__initial_schema.sql
        ├── V2__add_indexes.sql
        └── V3__add_request_errors.sql
```

---

## 🔧 Configuração (application.properties)

```properties
# Application
spring.application.name=monitor-api
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/monitoring
spring.datasource.username=monitor
spring.datasource.password=monitor123
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=monitor
spring.rabbitmq.password=monitor123

# Quartz (in-memory)
spring.quartz.job-store-type=memory
spring.quartz.properties.org.quartz.threadPool.threadCount=5

# Email (opcional - para relatórios)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${EMAIL_USERNAME:}
spring.mail.password=${EMAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Logging
logging.level.com.monitoring=INFO
logging.level.org.springframework.web=INFO
```

---

## 📦 Componentes Principais

### 1. Controllers

#### **SiteController.java**
```java
@RestController
@RequestMapping("/api/sites")
public class SiteController {
    // GET /api/sites - Lista todos
    // GET /api/sites/{id} - Busca por ID
    // POST /api/sites - Cria site
    // PUT /api/sites/{id} - Atualiza site
    // DELETE /api/sites/{id} - Remove site
    // POST /api/sites/{id}/check - Executa check manual
}
```

**Funcionalidades:**
- CRUD completo de sites
- Parsing de URL completa (extrai baseUrl + path)
- Trigger manual de checks via RabbitMQ
- Validação de entrada

#### **DashboardController.java**
```java
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    // GET /overview - Overview com métricas agregadas
    // GET /timeseries/errors - Série temporal de erros
    // GET /timeseries/perf - Série temporal de performance
}
```

**Funcionalidades:**
- Agregação de métricas por período (1h, 6h, 24h, 7d, 30d)
- Cálculo de percentis (P50, P95, P99)
- Contagem de issues por severidade
- Time bucketing para gráficos

#### **RunController.java**
```java
@RestController
@RequestMapping("/api/runs")
public class RunController {
    // GET /api/runs - Lista runs com filtros
    // GET /api/runs/{id} - Detalhes completos de run
}
```

**Funcionalidades:**
- Listagem de runs por período
- Force loading de lazy collections (failures, pageResults)
- Conversão para DTOs com failures incluídas

#### **LiveController.java**
```java
@RestController
@RequestMapping("/api/live")
public class LiveController {
    // GET /api/live?url={url} - SSE stream
}
```

**Funcionalidades:**
- Server-Sent Events para feedback real-time
- Cria site temporário para URL fornecida
- Publica check no RabbitMQ
- Polling do banco até conclusão
- Envia eventos: status, progress, completed, error

---

### 2. Services

#### **CheckService.java**
Orquestra a execução de checks.

```java
@Service
public class CheckService {
    /**
     * Publica check request no RabbitMQ para execução assíncrona.
     * Usado tanto por checks agendados quanto manuais.
     */
    public void requestCheck(Long siteId) {
        Site site = siteRepository.findById(siteId)
            .orElseThrow(() -> new RuntimeException("Site not found"));
        
        checkProducer.publishCheckRequest(siteId);
    }
}
```

#### **DashboardService.java**
Agrega dados para o dashboard.

```java
@Service
public class DashboardService {
    /**
     * Retorna overview do site com:
     * - Status atual (UP/DOWN)
     * - Uptime percentage
     * - Issues por severidade
     * - Métricas de performance (P50, P95, P99)
     * - Última execução
     */
    public DashboardOverviewDTO getOverview(Long siteId, String range);
    
    /**
     * Retorna série temporal de erros agrupados por bucket.
     * Ex: erros por hora nas últimas 24h
     */
    public TimeSeriesDTO getErrorsTimeSeries(Long siteId, String range, String bucket);
    
    /**
     * Retorna série temporal de performance (load time).
     * Ex: P95 load time a cada 6h nos últimos 7 dias
     */
    public TimeSeriesDTO getPerformanceTimeSeries(Long siteId, String range, String bucket);
}
```

#### **SiteService.java**
Lógica de negócio para sites.

```java
@Service
public class SiteService {
    /**
     * Cria site e suas páginas.
     * Se URL tem path, cria página automaticamente.
     */
    public Site createSite(CreateSiteRequest request);
    
    /**
     * Valida e parseia URL completa.
     * Extrai baseUrl e path.
     */
    private UrlParts parseUrl(String fullUrl);
}
```

#### **EmailService.java**
Envio de emails (relatórios semanais).

```java
@Service
public class EmailService {
    /**
     * Envia relatório semanal com:
     * - Resumo de uptime
     * - Top 5 issues
     * - Tendências de performance
     */
    public void sendWeeklyReport(Long siteId, String recipientEmail);
}
```

---

### 3. Scheduler

#### **AutoCheckJob.java**
Job Quartz que executa checks automaticamente.

```java
@DisallowConcurrentExecution
public class AutoCheckJob extends QuartzJobBean {
    /**
     * Executado a cada minuto (cron: 0 * * * * ?).
     * 
     * 1. Busca todos os sites ativos
     * 2. Para cada site, publica check request no RabbitMQ
     * 3. RabbitMQ garante processamento assíncrono
     */
    @Override
    protected void executeInternal(JobExecutionContext context) {
        List<Site> activeSites = siteRepository.findByIsActiveTrue();
        
        for (Site site : activeSites) {
            checkService.requestCheck(site.getId());
        }
    }
}
```

#### **WeeklyReportScheduler.java**
Envia relatórios semanais por email.

```java
@Component
public class WeeklyReportScheduler {
    /**
     * Executado todo domingo às 20h.
     * 
     * 1. Busca todos os sites ativos
     * 2. Para cada site com email configurado, envia relatório
     */
    @Scheduled(cron = "0 0 20 * * SUN")
    public void sendWeeklyReports() {
        List<Site> sites = siteRepository.findByIsActiveTrue();
        
        for (Site site : sites) {
            if (site.getNotificationEmail() != null) {
                emailService.sendWeeklyReport(site.getId(), site.getNotificationEmail());
            }
        }
    }
}
```

---

### 4. Messaging

#### **CheckProducer.java**
Publica mensagens de check no RabbitMQ.

```java
@Component
public class CheckProducer {
    /**
     * Publica check request para o runner processar.
     * 
     * Exchange: monitoring.exchange
     * Routing Key: monitoring.check
     * Queue: monitoring.checks
     */
    public void publishCheckRequest(Long siteId) {
        CheckRequestMessage message = new CheckRequestMessage(siteId);
        rabbitTemplate.convertAndSend(
            "monitoring.exchange",
            "monitoring.check",
            message
        );
    }
}
```

---

### 5. Domain Entities

#### **Site.java**
```java
@Entity
@Table(name = "sites")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;           // Nome amigável
    private String baseUrl;        // URL base (https://example.com)
    private Integer checkIntervalMinutes;  // Intervalo de check
    private Boolean isActive;      // Se está ativo
    private String notificationEmail;  // Email para alertas
    
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<SitePage> pages;  // Páginas do site
    
    @OneToMany(mappedBy = "site")
    private List<Run> runs;        // Histórico de execuções
}
```

#### **Run.java**
```java
@Entity
@Table(name = "runs")
public class Run {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;
    
    private LocalDateTime startedAt;   // Início da execução
    private LocalDateTime endedAt;     // Fim da execução
    
    @Enumerated(EnumType.STRING)
    private RunStatus status;          // SUCCESS, FAILED, etc
    
    private Integer criticalCount;     // Contagem de críticos
    private Integer majorCount;        // Contagem de majors
    private Integer minorCount;        // Contagem de minors
    private String summary;            // Resumo textual
    
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL)
    private List<PageResult> pageResults;  // Resultados por página
    
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL)
    private List<Failure> failures;    // Falhas detectadas
    
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL)
    private List<RequestError> requestErrors;  // Erros HTTP
}
```

#### **Failure.java**
```java
@Entity
@Table(name = "failures")
public class Failure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private Run run;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id")
    private SitePage page;
    
    @Enumerated(EnumType.STRING)
    private Severity severity;      // CRITICAL, MAJOR, MINOR
    
    @Enumerated(EnumType.STRING)
    private FailureType type;       // REQUEST_FAILED, JS_ERROR, etc
    
    private String message;         // Mensagem de erro
    private String url;             // URL onde ocorreu
    private LocalDateTime createdAt;
}
```

---

## 🎨 Frontend (static/)

### **index.html** - Dashboard Principal
```html
<!DOCTYPE html>
<html lang="pt-BR">
<!-- 
  Dashboard com Chart.js mostrando:
  - 4 cards de métricas (Status, Uptime, P95, Última Execução)
  - 3 gráficos (Issues por Severidade, Erros ao Longo do Tempo, Performance)
  - Lista detalhada de erros (últimas 24h)
  - Recomendações de melhoria
  - Problemas recentes
  
  Auto-refresh a cada 30 segundos
-->
<body>
  <!-- Selector de sites -->
  <select id="siteSelector" onchange="loadDashboard()"></select>
  
  <!-- Charts com Chart.js -->
  <canvas id="severityChart"></canvas>
  <canvas id="errorsChart"></canvas>
  <canvas id="perfChart"></canvas>
  
  <!-- Tabela de erros -->
  <table id="errorsTable"></table>
</body>
</html>
```

**JavaScript Principal:**
```javascript
// Carrega sites no dropdown
async function loadSites() {
  const sites = await fetch('/api/sites').then(r => r.json());
  // Popula selector
}

// Carrega dados do dashboard
async function loadDashboard() {
  const siteId = document.getElementById('siteSelector').value;
  
  // Overview
  const overview = await fetch(`/api/dashboard/overview?siteId=${siteId}&range=24h`)
    .then(r => r.json());
  updateOverview(overview);
  
  // Timeseries
  const errors = await fetch(`/api/dashboard/timeseries/errors?siteId=${siteId}&range=24h&bucket=1h`)
    .then(r => r.json());
  updateErrorsChart(errors);
  
  const perf = await fetch(`/api/dashboard/timeseries/perf?siteId=${siteId}&range=7d&bucket=6h`)
    .then(r => r.json());
  updatePerfChart(perf);
  
  // Erros detalhados
  loadErrorsList(siteId);
}

// Auto-refresh a cada 30s
setInterval(() => {
  if (siteId) loadDashboard();
}, 30000);
```

### **live.html** - Monitoramento ao Vivo
```html
<!DOCTYPE html>
<html lang="pt-BR">
<!--
  Monitoramento real-time via SSE:
  - Input para URL
  - Stream de eventos ao vivo
  - Resultados detalhados
  - Histórico de URLs (localStorage)
  - Guias de solução para erros
  - Botões de ação (Ver Dashboard, Nova Execução)
-->
<body>
  <!-- Input de URL -->
  <input type="url" id="urlInput" placeholder="https://example.com">
  <button onclick="startMonitoring()">Monitorar</button>
  
  <!-- Status stream -->
  <div id="statusContainer"></div>
  
  <!-- Resultados -->
  <div id="resultsContainer"></div>
  
  <!-- Histórico -->
  <div id="historyContainer"></div>
</body>
</html>
```

**JavaScript SSE:**
```javascript
function startMonitoring() {
  const url = document.getElementById('urlInput').value;
  
  // Abre conexão SSE
  const eventSource = new EventSource(`/api/live?url=${encodeURIComponent(url)}`);
  
  // Event listeners
  eventSource.addEventListener('status', (e) => {
    const data = JSON.parse(e.data);
    updateStatus(data.message);
  });
  
  eventSource.addEventListener('completed', (e) => {
    const data = JSON.parse(e.data);
    displayResults(data.result);
    eventSource.close();
    saveToHistory(url);
  });
  
  eventSource.addEventListener('error', (e) => {
    const data = JSON.parse(e.data);
    displayError(data.message);
    eventSource.close();
  });
}
```

---

## 🗄️ Database (Flyway Migrations)

### **V1__initial_schema.sql**
```sql
-- Cria tabelas: sites, site_pages, runs, page_results, failures
CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    check_interval_minutes INTEGER NOT NULL DEFAULT 5,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notification_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE runs (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id),
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    critical_count INTEGER NOT NULL DEFAULT 0,
    major_count INTEGER NOT NULL DEFAULT 0,
    minor_count INTEGER NOT NULL DEFAULT 0,
    summary TEXT
);
-- ... outras tabelas
```

### **V2__add_indexes.sql**
```sql
-- Índices para performance
CREATE INDEX idx_runs_site_started ON runs(site_id, started_at DESC);
CREATE INDEX idx_failures_run ON failures(run_id);
CREATE INDEX idx_page_results_run ON page_results(run_id);
```

---

## 🧪 Testes

**Estrutura de testes:**
```
src/test/java/com/monitoring/api/
├── controller/
│   ├── SiteControllerTest.java
│   └── DashboardControllerTest.java
├── service/
│   ├── CheckServiceTest.java
│   └── DashboardServiceTest.java
└── integration/
    └── FullFlowIntegrationTest.java
```

**Executar testes:**
```bash
mvn test
```

---

## 📊 Métricas e Monitoramento

### Actuator Endpoints
```properties
management.endpoints.web.exposure.include=health,info,metrics
```

**Endpoints disponíveis:**
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Informações da aplicação
- `GET /actuator/metrics` - Métricas JVM, HTTP, DB

---

## 🔍 Troubleshooting

### JSON serialization vazia
**Problema:** Lazy loading do JPA não carrega coleções.

**Solução:** Force loading antes de serializar:
```java
run.getFailures().size();  // Força hydrate
```

### CORS errors
**Problema:** Frontend em porta diferente.

**Solução:** Configurado em `WebConfig.java`:
```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
        .allowedOrigins("http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE");
}
```

---

**Versão:** 1.0  
**Última atualização:** 02/02/2026
