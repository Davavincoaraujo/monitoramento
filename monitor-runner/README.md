# 🤖 Monitor Runner - Documentação

## Visão Geral

Módulo executor responsável por:
- **Browser Automation**: Execução de testes sintéticos usando Playwright
- **Metrics Collection**: Coleta de métricas de performance e erros
- **RabbitMQ Consumer**: Consome mensagens de check requests
- **Data Persistence**: Salva resultados no PostgreSQL

**Porta:** 8081  
**Browser:** Firefox (padrão)

---

## 📁 Estrutura de Pastas

```
monitor-runner/
└── src/main/java/com/monitoring/runner/
    ├── config/                        # Configurações
    │   ├── PlaywrightConfig.java     # Bean do Playwright
    │   └── RabbitMQConfig.java       # Queues, exchanges
    │
    ├── messaging/                     # RabbitMQ Consumer
    │   ├── CheckListener.java        # Listener principal
    │   └── dto/
    │       └── CheckRequestMessage.java
    │
    ├── playwright/                    # Playwright Executor
    │   ├── PlaywrightExecutor.java   # Executor principal
    │   ├── MetricsCollector.java     # Coleta métricas
    │   └── ErrorDetector.java        # Detecta erros
    │
    ├── dto/                           # Data Transfer Objects
    │   ├── CheckResult.java          # Resultado completo
    │   ├── PageMetrics.java          # Métricas por página
    │   └── FailureInfo.java          # Info de falha
    │
    ├── domain/                        # Domain (compartilhado com API)
    │   ├── entity/                   # Mesmas entidades da API
    │   └── repository/               # Mesmos repositories
    │
    └── MonitorRunnerApplication.java  # Main class
```

---

## 🔧 Configuração (application.properties)

```properties
# Application
spring.application.name=monitor-runner
server.port=8081

# Database (mesmo do monitor-api)
spring.datasource.url=jdbc:postgresql://localhost:5432/monitoring
spring.datasource.username=monitor
spring.datasource.password=monitor123
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=monitor
spring.rabbitmq.password=monitor123

# Playwright
playwright.browser=firefox
playwright.headless=true
playwright.timeout=30000

# Performance
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10

# Logging
logging.level.com.monitoring=INFO
logging.level.com.microsoft.playwright=WARN
```

---

## 📦 Componentes Principais

### 1. Messaging Layer

#### **CheckListener.java**
Consumer RabbitMQ que processa check requests.

```java
@Component
public class CheckListener {
    
    private final PlaywrightExecutor playwrightExecutor;
    private final RunRepository runRepository;
    private final SiteRepository siteRepository;
    
    /**
     * Consome mensagens da queue "monitoring.checks".
     * 
     * Fluxo:
     * 1. Recebe siteId
     * 2. Busca site no banco
     * 3. Cria Run com status RUNNING
     * 4. Delega para PlaywrightExecutor
     * 5. Atualiza Run com resultados
     * 6. ACK message
     * 
     * Em caso de erro:
     * - Salva Run com status ERROR
     * - NACK message (requeue até 3 tentativas)
     */
    @RabbitListener(queues = "monitoring.checks")
    public void handleCheckRequest(CheckRequestMessage message) {
        log.info("Recebido check request para siteId: {}", message.getSiteId());
        
        try {
            // 1. Buscar site
            Site site = siteRepository.findById(message.getSiteId())
                .orElseThrow(() -> new RuntimeException("Site não encontrado"));
            
            // 2. Criar Run
            Run run = Run.builder()
                .site(site)
                .startedAt(LocalDateTime.now())
                .status(RunStatus.RUNNING)
                .build();
            run = runRepository.save(run);
            
            // 3. Executar check com Playwright
            CheckResult result = playwrightExecutor.executeCheck(site, run);
            
            // 4. Atualizar Run com resultados
            run.setEndedAt(LocalDateTime.now());
            run.setStatus(determineStatus(result));
            run.setCriticalCount(result.getCriticalCount());
            run.setMajorCount(result.getMajorCount());
            run.setMinorCount(result.getMinorCount());
            run.setSummary(buildSummary(result));
            runRepository.save(run);
            
            log.info("Check concluído para siteId: {} com status: {}", 
                message.getSiteId(), run.getStatus());
            
        } catch (Exception e) {
            log.error("Erro ao processar check para siteId: {}", 
                message.getSiteId(), e);
            throw new RuntimeException("Check failed", e);
        }
    }
    
    /**
     * Determina status final do run baseado em contagens de issues.
     */
    private RunStatus determineStatus(CheckResult result) {
        if (result.getCriticalCount() > 0 || result.getMajorCount() > 0) {
            return RunStatus.FAILED;
        } else if (result.getMinorCount() > 0) {
            return RunStatus.WARNING;
        } else {
            return RunStatus.SUCCESS;
        }
    }
}
```

---

### 2. Playwright Executor

#### **PlaywrightExecutor.java**
Executor principal que orquestra navegação e coleta de dados.

```java
@Component
public class PlaywrightExecutor {
    
    private final Playwright playwright;
    private final MetricsCollector metricsCollector;
    private final ErrorDetector errorDetector;
    private final String browserType; // firefox, chromium, webkit
    
    /**
     * Executa check completo do site.
     * 
     * Fluxo:
     * 1. Launch browser
     * 2. Create context
     * 3. Para cada página do site:
     *    a. Navigate
     *    b. Coletar métricas
     *    c. Detectar erros
     *    d. Salvar PageResult
     * 4. Close browser
     * 5. Agregar resultados
     * 
     * @param site Site a ser testado
     * @param run Run atual
     * @return CheckResult com todos os dados coletados
     */
    public CheckResult executeCheck(Site site, Run run) {
        log.info("Iniciando check para site: {} ({})", site.getName(), site.getBaseUrl());
        
        Browser browser = null;
        CheckResult checkResult = new CheckResult();
        
        try {
            // 1. Launch browser
            browser = launchBrowser();
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("MonitorBot/1.0")
                .setViewportSize(1920, 1080));
            
            Page page = context.newPage();
            
            // 2. Setup error listeners
            setupErrorListeners(page, checkResult);
            
            // 3. Para cada página do site
            List<SitePage> pages = site.getPages().stream()
                .filter(SitePage::getIsActive)
                .toList();
            
            for (SitePage sitePage : pages) {
                PageMetrics metrics = checkPage(page, site, sitePage, run, checkResult);
                checkResult.addPageMetrics(metrics);
            }
            
            log.info("Check concluído para site: {} - {} páginas testadas", 
                site.getName(), pages.size());
            
        } catch (Exception e) {
            log.error("Erro ao executar check para site: {}", site.getName(), e);
            checkResult.addCriticalFailure(
                "Check execution failed: " + e.getMessage()
            );
        } finally {
            if (browser != null) {
                browser.close();
            }
        }
        
        return checkResult;
    }
    
    /**
     * Testa uma página específica.
     */
    private PageMetrics checkPage(Page page, Site site, SitePage sitePage, 
                                   Run run, CheckResult checkResult) {
        String url = site.getBaseUrl() + sitePage.getPath();
        log.info("Testando página: {} - {}", sitePage.getName(), url);
        
        PageMetrics metrics = new PageMetrics();
        metrics.setPageName(sitePage.getName());
        metrics.setUrl(url);
        
        try {
            // 1. Navigate com timing
            long startTime = System.currentTimeMillis();
            Response response = page.navigate(url, new Page.NavigateOptions()
                .setTimeout(30000)
                .setWaitUntil(WaitUntilState.LOAD));
            long loadTime = System.currentTimeMillis() - startTime;
            
            // 2. Coletar métricas de performance
            metricsCollector.collectPerformanceMetrics(page, metrics);
            metrics.setLoadTimeMs((int) loadTime);
            
            // 3. Detectar erros JavaScript
            errorDetector.detectJavaScriptErrors(page, checkResult, sitePage);
            
            // 4. Validar response
            if (response == null || !response.ok()) {
                checkResult.addFailure(
                    sitePage,
                    Severity.CRITICAL,
                    FailureType.NAVIGATION_FAILED,
                    "Navigation failed: " + (response != null ? response.status() : "No response")
                );
            }
            
            // 5. Salvar PageResult no banco
            savePageResult(run, sitePage, metrics);
            
        } catch (PlaywrightException e) {
            log.error("Erro ao testar página: {}", url, e);
            checkResult.addFailure(
                sitePage,
                Severity.CRITICAL,
                FailureType.NAVIGATION_FAILED,
                "Playwright error: " + e.getMessage()
            );
        }
        
        return metrics;
    }
    
    /**
     * Launch browser com fallback.
     * Tenta Firefox primeiro, depois WebKit se falhar.
     */
    private Browser launchBrowser() {
        try {
            if ("firefox".equals(browserType)) {
                return playwright.firefox().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setTimeout(30000));
            } else if ("webkit".equals(browserType)) {
                return playwright.webkit().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));
            } else {
                return playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));
            }
        } catch (Exception e) {
            log.warn("Falha ao lançar {}, tentando WebKit", browserType);
            return playwright.webkit().launch(new BrowserType.LaunchOptions()
                .setHeadless(true));
        }
    }
    
    /**
     * Setup listeners para capturar erros em tempo real.
     */
    private void setupErrorListeners(Page page, CheckResult checkResult) {
        // Console errors
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                checkResult.addConsoleError(msg.text());
            }
        });
        
        // Page errors (uncaught exceptions)
        page.onPageError(error -> {
            checkResult.addJavaScriptError(error.getMessage());
        });
        
        // Request failures
        page.onRequestFailed(request -> {
            checkResult.addRequestFailure(
                request.url(),
                request.failure()
            );
        });
    }
}
```

---

#### **MetricsCollector.java**
Coleta métricas de performance da página.

```java
@Component
public class MetricsCollector {
    
    /**
     * Coleta métricas de performance usando Navigation Timing API.
     * 
     * Métricas coletadas:
     * - TTFB (Time to First Byte)
     * - Load Time (total)
     * - DOM Content Loaded Time
     * - Request Count
     * - Failed Request Count
     */
    public void collectPerformanceMetrics(Page page, PageMetrics metrics) {
        try {
            // Executar JavaScript para coletar Navigation Timing
            Object timingObj = page.evaluate(
                "() => JSON.stringify(window.performance.timing)"
            );
            
            if (timingObj != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode timing = mapper.readTree(timingObj.toString());
                
                // Calcular métricas
                long navigationStart = timing.get("navigationStart").asLong();
                long responseStart = timing.get("responseStart").asLong();
                long domContentLoaded = timing.get("domContentLoadedEventEnd").asLong();
                long loadComplete = timing.get("loadEventEnd").asLong();
                
                // TTFB
                int ttfb = (int) (responseStart - navigationStart);
                metrics.setTtfbMs(ttfb);
                
                // DOM Content Loaded
                int domTime = (int) (domContentLoaded - navigationStart);
                metrics.setDomContentMs(domTime);
                
                // Total Load Time
                int loadTime = (int) (loadComplete - navigationStart);
                metrics.setLoadTimeMs(loadTime);
            }
            
            // Coletar contagem de requests
            collectResourceMetrics(page, metrics);
            
        } catch (Exception e) {
            log.warn("Erro ao coletar métricas de performance", e);
        }
    }
    
    /**
     * Coleta métricas de recursos (requests HTTP).
     */
    private void collectResourceMetrics(Page page, PageMetrics metrics) {
        try {
            Object resourcesObj = page.evaluate(
                "() => performance.getEntriesByType('resource').length"
            );
            
            if (resourcesObj != null) {
                metrics.setRequestCount(Integer.parseInt(resourcesObj.toString()));
            }
        } catch (Exception e) {
            log.warn("Erro ao coletar métricas de recursos", e);
        }
    }
}
```

---

#### **ErrorDetector.java**
Detecta diferentes tipos de erros durante navegação.

```java
@Component
public class ErrorDetector {
    
    private final FailureRepository failureRepository;
    private final RequestErrorRepository requestErrorRepository;
    
    /**
     * Detecta erros JavaScript na página.
     * Busca por erros no console e exceptions não capturadas.
     */
    public void detectJavaScriptErrors(Page page, CheckResult checkResult, 
                                       SitePage sitePage) {
        try {
            // Avaliar se há erros no console
            Object hasErrors = page.evaluate(
                "() => {" +
                "  const errors = [];" +
                "  const originalConsoleError = console.error;" +
                "  console.error = function(...args) {" +
                "    errors.push(args.join(' '));" +
                "    originalConsoleError.apply(console, args);" +
                "  };" +
                "  return errors;" +
                "}"
            );
            
            // Processar erros encontrados
            if (hasErrors != null) {
                List<String> errors = (List<String>) hasErrors;
                for (String error : errors) {
                    checkResult.addFailure(
                        sitePage,
                        Severity.MAJOR,
                        FailureType.CONSOLE_ERROR,
                        error
                    );
                }
            }
            
        } catch (Exception e) {
            log.warn("Erro ao detectar JavaScript errors", e);
        }
    }
    
    /**
     * Salva erros de request HTTP no banco.
     */
    public void saveRequestErrors(Run run, SitePage page, 
                                   List<Request> failedRequests) {
        for (Request request : failedRequests) {
            RequestError error = new RequestError();
            error.setRun(run);
            error.setPage(page);
            error.setUrl(request.url());
            error.setMethod(request.method());
            error.setStatusCode(0); // Failed before response
            error.setErrorText(request.failure());
            requestErrorRepository.save(error);
        }
    }
}
```

---

### 3. Configuration

#### **PlaywrightConfig.java**
Configura e gerencia instância do Playwright.

```java
@Configuration
public class PlaywrightConfig {
    
    @Value("${playwright.browser:firefox}")
    private String browserType;
    
    /**
     * Cria bean Playwright singleton.
     * Playwright é thread-safe e deve ser compartilhado.
     */
    @Bean
    public Playwright playwright() {
        return Playwright.create();
    }
    
    /**
     * Registra hook para fechar Playwright ao shutdown.
     */
    @PreDestroy
    public void cleanup() {
        Playwright pw = playwright();
        if (pw != null) {
            pw.close();
        }
    }
}
```

---

### 4. DTOs

#### **CheckResult.java**
Agrega todos os resultados de um check.

```java
public class CheckResult {
    private List<PageMetrics> pageMetrics = new ArrayList<>();
    private List<FailureInfo> failures = new ArrayList<>();
    private List<String> consoleErrors = new ArrayList<>();
    private List<String> jsErrors = new ArrayList<>();
    
    private int criticalCount = 0;
    private int majorCount = 0;
    private int minorCount = 0;
    
    /**
     * Adiciona falha e incrementa contador de severidade.
     */
    public void addFailure(SitePage page, Severity severity, 
                          FailureType type, String message) {
        failures.add(new FailureInfo(page, severity, type, message));
        
        switch (severity) {
            case CRITICAL -> criticalCount++;
            case MAJOR -> majorCount++;
            case MINOR -> minorCount++;
        }
    }
    
    public void addPageMetrics(PageMetrics metrics) {
        pageMetrics.add(metrics);
    }
    
    // Getters...
}
```

#### **PageMetrics.java**
Métricas coletadas de uma página.

```java
public class PageMetrics {
    private String pageName;
    private String url;
    private Integer ttfbMs;          // Time to First Byte
    private Integer loadTimeMs;      // Total load time
    private Integer domContentMs;    // DOMContentLoaded
    private Integer requestCount;    // Total requests
    private Integer failedRequestCount;
    private Integer jsErrorCount;
    private Integer consoleErrorCount;
    
    // Getters and Setters...
}
```

---

## 🎭 Playwright - Detalhes

### Browsers Suportados

1. **Firefox** (Recomendado)
   - ✅ Estável em ARM64 macOS
   - ✅ Boa performance
   - ✅ Sem crashes

2. **WebKit** (Fallback)
   - ✅ Nativo em macOS
   - ⚠️ Algumas limitações

3. **Chromium** (Não recomendado)
   - ❌ Crashes em ARM64 macOS
   - ✅ OK em Linux/Windows

### Configuração de Browser

```properties
# application.properties
playwright.browser=firefox  # ou webkit, chromium
playwright.headless=true    # false para debug visual
playwright.timeout=30000    # timeout padrão (ms)
```

### Troubleshooting Playwright

#### Instalar browsers manualmente
```bash
# Instalar todos os browsers
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

# Instalar apenas Firefox
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install firefox"
```

#### Debug visual (headless=false)
```properties
playwright.headless=false
```

#### Logs detalhados
```properties
logging.level.com.microsoft.playwright=DEBUG
```

---

## 🔄 Fluxo Completo

```
1. monitor-api publica: CheckRequestMessage(siteId=1)
   └─> RabbitMQ Queue: monitoring.checks

2. CheckListener consome mensagem
   ├─> Busca Site(id=1) no banco
   ├─> Cria Run(status=RUNNING)
   └─> Chama PlaywrightExecutor.executeCheck()

3. PlaywrightExecutor
   ├─> Launch Firefox
   ├─> Para cada página do site:
   │   ├─> Navigate
   │   ├─> MetricsCollector.collectPerformanceMetrics()
   │   ├─> ErrorDetector.detectJavaScriptErrors()
   │   └─> Salva PageResult no banco
   ├─> Close browser
   └─> Retorna CheckResult

4. CheckListener finaliza
   ├─> Atualiza Run(status=SUCCESS/FAILED/WARNING)
   ├─> Salva failures no banco
   └─> ACK mensagem RabbitMQ
```

---

## 📊 Métricas Coletadas

| Métrica | Descrição | Fonte |
|---------|-----------|-------|
| **TTFB** | Time to First Byte | Navigation Timing API |
| **Load Time** | Tempo total de carregamento | Navigation Timing API |
| **DOM Content** | DOMContentLoaded event | Navigation Timing API |
| **Request Count** | Total de requests HTTP | Resource Timing API |
| **Failed Requests** | Requests que falharam | Playwright listeners |
| **JS Errors** | Erros JavaScript | page.onPageError |
| **Console Errors** | console.error() calls | page.onConsoleMessage |

---

## ⚠️ Error Detection

### Tipos de Erros Detectados

#### 1. REQUEST_FAILED
```java
page.onRequestFailed(request -> {
    // Request HTTP falhou (timeout, abort, network)
    Failure failure = new Failure(
        severity: CRITICAL,
        type: REQUEST_FAILED,
        message: "Request failed: " + request.failure(),
        url: request.url()
    );
});
```

#### 2. JS_ERROR
```java
page.onPageError(error -> {
    // Erro JavaScript não capturado
    Failure failure = new Failure(
        severity: MAJOR,
        type: JS_ERROR,
        message: error.getMessage()
    );
});
```

#### 3. CONSOLE_ERROR
```java
page.onConsoleMessage(msg -> {
    if ("error".equals(msg.type())) {
        // Erro no console do browser
        Failure failure = new Failure(
            severity: MINOR,
            type: CONSOLE_ERROR,
            message: msg.text()
        );
    }
});
```

#### 4. NAVIGATION_FAILED
```java
try {
    page.navigate(url);
} catch (PlaywrightException e) {
    // Falha ao navegar
    Failure failure = new Failure(
        severity: CRITICAL,
        type: NAVIGATION_FAILED,
        message: "Navigation error: " + e.getMessage()
    );
}
```

---

## 🧪 Testes

```bash
# Executar testes
mvn test

# Testar com browser visível
mvn test -Dplaywright.headless=false
```

---

## 🔍 Troubleshooting

### Browser crashes
**Solução:** Mudar de browser em `application.properties`
```properties
playwright.browser=webkit  # em vez de chromium
```

### Timeouts frequentes
**Solução:** Aumentar timeout
```properties
playwright.timeout=60000  # 60 segundos
```

### Muitos erros REQUEST_FAILED
**Causa:** Sites bloqueiam bots
**Solução:** Configurar User-Agent realista
```java
context.newContext(new Browser.NewContextOptions()
    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."));
```

---

**Versão:** 1.0  
**Última atualização:** 02/02/2026
