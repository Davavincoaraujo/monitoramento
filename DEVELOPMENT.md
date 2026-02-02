# 🛠️ Guia de Desenvolvimento

## Setup do Ambiente

### 1. Pré-requisitos

```bash
# Java 21
java -version
# Deve mostrar: openjdk version "21.x.x"

# Maven 3.9+
mvn -version

# Docker Desktop
docker --version
docker-compose --version

# Git
git --version
```

### 2. Clonar Repositório

```bash
git clone <repository-url>
cd Monitoramento
```

### 3. Configurar IDE

#### IntelliJ IDEA (Recomendado)

1. **Importar Projeto**
   - File → Open → Selecionar pasta `Monitoramento`
   - Maven auto-import irá configurar

2. **Configurar Java 21**
   - File → Project Structure → Project SDK → Add SDK → Download JDK 21

3. **Plugins Recomendados**
   - Lombok
   - Spring Boot
   - Database Navigator

4. **Configurar Code Style**
   - Settings → Editor → Code Style → Import Scheme
   - Usar Google Java Style Guide

#### VS Code

1. **Extensões Necessárias**
   ```
   - Extension Pack for Java (Microsoft)
   - Spring Boot Extension Pack (Pivotal)
   - Lombok Annotations Support
   ```

2. **settings.json**
   ```json
   {
     "java.home": "/path/to/java-21",
     "java.configuration.updateBuildConfiguration": "automatic",
     "spring-boot.ls.problem.boot.validation.enable": true
   }
   ```

### 4. Iniciar Infraestrutura

```bash
# Subir PostgreSQL + RabbitMQ
docker-compose up -d

# Verificar status
docker-compose ps

# Ver logs
docker-compose logs -f
```

### 5. Setup do Banco de Dados

```bash
# Criar schema inicial
docker exec -i monitor-postgres psql -U monitor -d monitoring < seed-data.sql

# Verificar tabelas
docker exec -it monitor-postgres psql -U monitor -d monitoring -c "\dt"
```

### 6. Compilar Projetos

```bash
# Compilar ambos os módulos
mvn clean install

# Ou compilar individualmente
cd monitor-api && mvn clean package
cd ../monitor-runner && mvn clean package
```

### 7. Executar Aplicações

#### Via Maven (Desenvolvimento)

```bash
# Terminal 1 - API
cd monitor-api
mvn spring-boot:run

# Terminal 2 - Runner
cd monitor-runner
mvn spring-boot:run
```

#### Via JAR (Produção)

```bash
# Terminal 1 - API
cd monitor-api
java -jar target/monitor-api-1.0.0.jar

# Terminal 2 - Runner
cd monitor-runner
java -jar target/monitor-runner-1.0.0.jar
```

#### Via IDE

**IntelliJ IDEA:**
1. Botão direito em `MonitorApiApplication.java` → Run
2. Botão direito em `MonitorRunnerApplication.java` → Run

---

## 🔄 Fluxo de Desenvolvimento

### 1. Criar Nova Feature

```bash
# Criar branch
git checkout -b feature/nome-da-feature

# Fazer alterações
# ...

# Commit
git add .
git commit -m "feat: descrição da feature"

# Push
git push origin feature/nome-da-feature
```

### 2. Padrões de Commit

Usar Conventional Commits:

```
feat: adicionar nova funcionalidade
fix: corrigir bug
docs: atualizar documentação
refactor: refatorar código sem mudar comportamento
test: adicionar ou atualizar testes
chore: tarefas de manutenção
```

### 3. Criar Nova Entidade

**Exemplo:** Adicionar entidade `Alert`

1. **Criar Entidade**
   ```java
   // monitor-api/src/main/java/com/monitoring/api/domain/entity/Alert.java
   @Entity
   @Table(name = "alerts")
   public class Alert {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       
       // campos...
   }
   ```

2. **Criar Repository**
   ```java
   // monitor-api/src/main/java/com/monitoring/api/domain/repository/AlertRepository.java
   @Repository
   public interface AlertRepository extends JpaRepository<Alert, Long> {
       List<Alert> findByStatusAndSiteId(String status, Long siteId);
   }
   ```

3. **Criar Migration**
   ```sql
   -- monitor-api/src/main/resources/db/migration/V4__add_alerts_table.sql
   CREATE TABLE alerts (
       id BIGSERIAL PRIMARY KEY,
       site_id BIGINT NOT NULL REFERENCES sites(id),
       message TEXT NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
   );
   ```

4. **Criar DTO**
   ```java
   // monitor-api/src/main/java/com/monitoring/api/dto/AlertDTO.java
   public record AlertDTO(
       Long id,
       String message,
       LocalDateTime createdAt
   ) {}
   ```

5. **Criar Controller**
   ```java
   // monitor-api/src/main/java/com/monitoring/api/controller/AlertController.java
   @RestController
   @RequestMapping("/api/alerts")
   public class AlertController {
       @GetMapping
       public List<AlertDTO> list() {
           // ...
       }
   }
   ```

### 4. Adicionar Novo Endpoint

**Exemplo:** GET /api/sites/{id}/health

```java
@RestController
@RequestMapping("/api/sites")
public class SiteController {
    
    @GetMapping("/{id}/health")
    public ResponseEntity<HealthDTO> getHealth(@PathVariable Long id) {
        Site site = siteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        HealthDTO health = healthService.calculateHealth(site);
        return ResponseEntity.ok(health);
    }
}
```

---

## 🧪 Testes

### Unit Tests

```bash
# Executar todos os testes
mvn test

# Executar classe específica
mvn test -Dtest=SiteControllerTest

# Executar método específico
mvn test -Dtest=SiteControllerTest#testCreateSite
```

### Integration Tests

```bash
# Executar testes de integração
mvn verify -P integration-tests
```

### Exemplo de Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class SiteControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testCreateSite() throws Exception {
        CreateSiteRequest request = new CreateSiteRequest(
            "Test Site",
            "https://example.com",
            5
        );
        
        mockMvc.perform(post("/api/sites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Site"));
    }
}
```

---

## 🐛 Debugging

### 1. Debug via IDE

**IntelliJ IDEA:**
1. Colocar breakpoint (clique na margem esquerda)
2. Botão direito → Debug
3. Usar Step Into (F7), Step Over (F8)

### 2. Remote Debugging

```bash
# Iniciar aplicação com debug
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar target/monitor-api-1.0.0.jar

# IDE: Run → Edit Configurations → Remote JVM Debug → Port 5005
```

### 3. Logs Detalhados

```properties
# application.properties
logging.level.com.monitoring=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### 4. Debugging Playwright

```properties
# Executar com browser visível
playwright.headless=false

# Logs Playwright
logging.level.com.microsoft.playwright=DEBUG

# Slow down (para ver ações)
# Adicionar no código:
page.setDefaultTimeout(5000);
page.setDefaultNavigationTimeout(30000);
```

---

## 📊 Monitoring & Profiling

### 1. Spring Boot Actuator

```properties
# application.properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

**Endpoints:**
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/metrics
- http://localhost:8080/actuator/prometheus

### 2. Database Monitoring

```bash
# Ver queries lentas
docker exec -it monitor-postgres psql -U monitor -d monitoring

# Ver conexões ativas
SELECT * FROM pg_stat_activity;

# Ver queries lentas
SELECT query, calls, total_time, mean_time 
FROM pg_stat_statements 
ORDER BY total_time DESC 
LIMIT 10;
```

### 3. RabbitMQ Monitoring

- Management UI: http://localhost:15672
- User: `monitor` / Pass: `monitor123`

**Métricas importantes:**
- Message rate
- Queue depth
- Consumer utilization

---

## 🔧 Troubleshooting Comum

### Porta já em uso

```bash
# Ver processo na porta 8080
lsof -ti:8080

# Matar processo
kill -9 $(lsof -ti:8080)
```

### Conexão recusada PostgreSQL

```bash
# Verificar se está rodando
docker ps | grep postgres

# Reiniciar
docker-compose restart postgres

# Ver logs
docker-compose logs postgres
```

### RabbitMQ não conecta

```bash
# Verificar status
docker-compose ps rabbitmq

# Ver logs
docker-compose logs rabbitmq

# Resetar
docker-compose down rabbitmq
docker-compose up -d rabbitmq
```

### Flyway migration falha

```bash
# Ver versão atual
docker exec -it monitor-postgres psql -U monitor -d monitoring \
  -c "SELECT * FROM flyway_schema_history;"

# Repair (último recurso)
mvn flyway:repair

# Clean e recriar (APAGA TUDO!)
docker-compose down -v
docker-compose up -d
docker exec -i monitor-postgres psql -U monitor -d monitoring < seed-data.sql
```

### OutOfMemoryError

```bash
# Aumentar heap
java -Xmx2g -Xms512m -jar target/monitor-api-1.0.0.jar

# Ver uso de memória
jps  # Pegar PID
jstat -gc <PID> 1000  # GC stats a cada 1s
```

---

## 📚 Recursos Úteis

### Documentação Oficial
- [Spring Boot Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Playwright Java](https://playwright.dev/java/docs/intro)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)

### Ferramentas
- [Postman](https://www.postman.com/) - Testar APIs
- [DBeaver](https://dbeaver.io/) - Cliente PostgreSQL
- [Insomnia](https://insomnia.rest/) - Alternativa ao Postman

### Extensões Chrome
- JSON Formatter
- ModHeader (para testar headers)
- React/Vue DevTools (se adicionar frontend)

---

## 🎯 Checklist Antes de Commit

- [ ] Código compila sem erros
- [ ] Testes passam (`mvn test`)
- [ ] Sem warnings do IDE
- [ ] Formatação correta (Google Java Style)
- [ ] Comentários adicionados para lógica complexa
- [ ] README atualizado se necessário
- [ ] Migration criada se mudou schema

---

## 🚀 Deploy

### Build para Produção

```bash
# Compilar com profile de produção
mvn clean package -Pprod

# Gerar imagens Docker
docker build -t monitor-api:latest ./monitor-api
docker build -t monitor-runner:latest ./monitor-runner
```

### Variáveis de Ambiente

```bash
# Production environment variables
export DATABASE_URL=jdbc:postgresql://prod-db:5432/monitoring
export DATABASE_USERNAME=monitor
export DATABASE_PASSWORD=<secure-password>
export RABBITMQ_HOST=prod-rabbitmq
export EMAIL_USERNAME=<email>
export EMAIL_PASSWORD=<app-password>
```

---

**Autor:** Sistema de Monitoramento  
**Última atualização:** 02/02/2026
