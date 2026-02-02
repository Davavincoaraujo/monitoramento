# 🛠️ Development Cheatsheet

## Quick Commands

### Start Everything
```bash
# Terminal 1 - Infrastructure
docker-compose up -d && sleep 15

# Terminal 2 - API
cd monitor-api && mvn spring-boot:run

# Terminal 3 - Runner
cd monitor-runner && mvn spring-boot:run

# Terminal 4 - Logs
tail -f monitor-api/target/*.log monitor-runner/target/*.log
```

### Stop Everything
```bash
# Ctrl+C em terminais Spring Boot
docker-compose down
```

---

## Database Commands

### Connect to PostgreSQL
```bash
psql -h localhost -U monitor -d monitoring
# Password: monitor123
```

### Common Queries
```sql
-- List all sites
SELECT id, name, base_url, enabled, frequency_seconds FROM sites;

-- List pages per site
SELECT s.name, p.name as page, p.path 
FROM sites s 
JOIN site_pages p ON p.site_id = s.id;

-- Recent runs
SELECT r.id, s.name, r.started_at, r.status, r.critical_count, r.major_count
FROM runs r
JOIN sites s ON s.id = r.site_id
ORDER BY r.started_at DESC
LIMIT 10;

-- Failures summary
SELECT s.name, f.severity, f.type, COUNT(*)
FROM failures f
JOIN runs r ON r.id = f.run_id
JOIN sites s ON s.id = r.site_id
WHERE r.started_at > NOW() - INTERVAL '24 hours'
GROUP BY s.name, f.severity, f.type
ORDER BY COUNT(*) DESC;

-- Site with no runs yet
SELECT s.id, s.name, s.base_url
FROM sites s
LEFT JOIN runs r ON r.site_id = s.id
WHERE r.id IS NULL AND s.enabled = true;

-- Performance metrics
SELECT p.name, 
       AVG(pr.ttfb_ms) as avg_ttfb,
       AVG(pr.load_ms) as avg_load,
       COUNT(*) as samples
FROM page_results pr
JOIN site_pages p ON p.id = pr.page_id
GROUP BY p.id, p.name
ORDER BY avg_load DESC;
```

### Insert Test Data
```bash
psql -h localhost -U monitor -d monitoring -f seed-data.sql
```

### Clean All Data (Keep Schema)
```sql
TRUNCATE request_errors, failures, page_results, runs, rules, site_pages, sites CASCADE;
```

---

## RabbitMQ Commands

### Web UI
http://localhost:15672
- User: `monitor`
- Pass: `monitor123`

### CLI via Docker
```bash
# List queues
docker exec monitor-rabbitmq rabbitmqctl list_queues

# Publish test message
docker exec monitor-rabbitmq rabbitmqadmin publish \
  routing_key=monitor.run-check \
  payload='{"siteId":1,"siteName":"Test","baseUrl":"https://example.com"}'

# Purge queue
docker exec monitor-rabbitmq rabbitmqctl purge_queue monitor.run-check
```

### REST API
```bash
# Get queue status
curl -u monitor:monitor123 http://localhost:15672/api/queues/%2F/monitor.run-check

# Manual publish (JSON)
curl -u monitor:monitor123 -X POST http://localhost:15672/api/exchanges/%2F/amq.default/publish \
  -H "Content-Type: application/json" \
  -d '{
    "properties":{},
    "routing_key":"monitor.run-check",
    "payload":"{\"siteId\":1,\"siteName\":\"Google\",\"baseUrl\":\"https://www.google.com\"}",
    "payload_encoding":"string"
  }'
```

---

## API Endpoints

### Sites
```bash
# List all
curl http://localhost:8080/api/sites

# Get one
curl http://localhost:8080/api/sites/1

# Get config (used by runner)
curl http://localhost:8080/api/sites/1/config
```

### Dashboard
```bash
# Overview
curl "http://localhost:8080/api/dashboard/overview?siteId=1&range=24h"

# Error timeseries
curl "http://localhost:8080/api/dashboard/timeseries/errors?siteId=1&range=24h&bucket=1h"

# Performance timeseries
curl "http://localhost:8080/api/dashboard/timeseries/perf?siteId=1&range=7d&bucket=1h"
```

### Runs
```bash
# List runs
curl "http://localhost:8080/api/runs?siteId=1&from=2024-01-01T00:00:00&to=2024-01-31T23:59:59"

# Get run details
curl http://localhost:8080/api/runs/1
```

### SSE
```bash
# Connect to events stream
curl -N http://localhost:8080/api/events?siteId=1

# Browser JS
# const es = new EventSource('http://localhost:8080/api/events?siteId=1');
# es.addEventListener('run_completed', e => console.log(e.data));
```

---

## Maven Commands

### Build
```bash
# monitor-api
cd monitor-api
mvn clean install

# monitor-runner
cd monitor-runner
mvn clean install
```

### Run
```bash
# monitor-api
cd monitor-api
mvn spring-boot:run

# With profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# With debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Test (when implemented)
```bash
mvn test
mvn test -Dtest=SpecificTest
```

### Package JAR
```bash
mvn clean package
java -jar target/monitor-api-1.0.0.jar
```

---

## Playwright Commands

### Install Browsers
```bash
cd monitor-runner

# Chromium only (recommended)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# All browsers
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

# Firefox only
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install firefox"

# Webkit only
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install webkit"
```

### Test Playwright (Java)
```bash
cd monitor-runner

# Run codegen (opens browser for recording)
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="codegen https://example.com"
```

---

## Docker Commands

### Start Services
```bash
# All services
docker-compose up -d

# Specific service
docker-compose up -d postgres
docker-compose up -d rabbitmq
docker-compose up -d pgadmin
```

### Stop Services
```bash
docker-compose down

# Remove volumes (delete data)
docker-compose down -v
```

### Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f postgres
docker-compose logs -f rabbitmq

# Last 100 lines
docker-compose logs --tail=100 postgres
```

### Restart Service
```bash
docker-compose restart postgres
docker-compose restart rabbitmq
```

### Execute Commands
```bash
# PostgreSQL
docker exec -it monitor-postgres psql -U monitor -d monitoring

# RabbitMQ
docker exec -it monitor-rabbitmq rabbitmqctl status
```

---

## Debugging Tips

### Enable Debug Logs (application.yml)
```yaml
logging:
  level:
    com.monitoring: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Spring Boot Actuator (add to pom.xml)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,loggers
```

```bash
# Endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/loggers
```

### IDE Debug
```bash
# monitor-api
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# monitor-runner
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006"
```

Then attach debugger to:
- monitor-api: localhost:5005
- monitor-runner: localhost:5006

---

## Testing Scenarios

### Manual Trigger Check
```sql
-- Force site to be "due"
UPDATE runs SET started_at = NOW() - INTERVAL '1 hour' 
WHERE site_id = 1 
ORDER BY started_at DESC 
LIMIT 1;

-- Wait for next Quartz cycle (max 1 minute)
-- Or publish directly to RabbitMQ (see RabbitMQ section)
```

### Simulate Failures
```sql
-- Insert fake site with broken URL
INSERT INTO sites (name, base_url, enabled, frequency_seconds, created_at, updated_at)
VALUES ('Broken Site', 'https://this-does-not-exist-12345.com', true, 60, NOW(), NOW());

INSERT INTO site_pages (site_id, name, path, enabled, created_at)
VALUES (
  (SELECT id FROM sites WHERE name = 'Broken Site'),
  'Home', '/', true, NOW()
);
```

### Test Email Report (Manual Trigger)
```java
// Add temporary endpoint to SiteController.java
@PostMapping("/debug/trigger-report/{siteId}")
public ResponseEntity<String> triggerReport(@PathVariable Long siteId) {
    Site site = siteRepository.findById(siteId).orElseThrow();
    weeklyReportService.generateAndSendReport(site);
    return ResponseEntity.ok("Report generated");
}
```

```bash
curl -X POST http://localhost:8080/api/sites/debug/trigger-report/1
```

### Load Test
```bash
# Insert 100 sites
for i in {1..100}; do
  psql -h localhost -U monitor -d monitoring -c "
    INSERT INTO sites (name, base_url, enabled, frequency_seconds, created_at, updated_at)
    VALUES ('Site $i', 'https://example.com', true, 300, NOW(), NOW());
    
    INSERT INTO site_pages (site_id, name, path, enabled, created_at)
    VALUES (currval('sites_id_seq'), 'Home $i', '/', true, NOW());
  "
done

# Monitor queue size
watch -n 1 'curl -s -u monitor:monitor123 http://localhost:15672/api/queues/%2F/monitor.run-check | jq .messages'
```

---

## Performance Tuning

### PostgreSQL Connection Pool
```yaml
# monitor-api application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### RabbitMQ Concurrency
```yaml
# monitor-runner application.yml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 5          # Start with 5 consumers
        max-concurrency: 10     # Max 10 consumers
        prefetch: 1             # Process 1 message at a time
```

### Playwright Timeouts
```yaml
# monitor-runner application.yml
playwright:
  timeout-ms: 30000           # 30s default
  viewport-width: 1920
  viewport-height: 1080
  headless: true              # false for debugging
```

---

## Backup & Restore

### Backup Database
```bash
# Full backup
docker exec monitor-postgres pg_dump -U monitor monitoring > backup_$(date +%Y%m%d).sql

# Schema only
docker exec monitor-postgres pg_dump -U monitor -s monitoring > schema.sql

# Data only
docker exec monitor-postgres pg_dump -U monitor -a monitoring > data.sql
```

### Restore Database
```bash
# Restore full backup
psql -h localhost -U monitor -d monitoring < backup_20240101.sql

# Restore via docker
cat backup_20240101.sql | docker exec -i monitor-postgres psql -U monitor -d monitoring
```

---

## Useful SQL Snippets

### Clean Old Data (Retention Policy)
```sql
-- Delete request_errors older than 30 days
DELETE FROM request_errors WHERE created_at < NOW() - INTERVAL '30 days';

-- Delete runs older than 90 days
DELETE FROM runs WHERE started_at < NOW() - INTERVAL '90 days';

-- Vacuum to reclaim space
VACUUM ANALYZE;
```

### Generate Report Data (Manual)
```sql
-- Uptime last 7 days
SELECT 
  COUNT(*) as total_runs,
  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_runs,
  ROUND(100.0 * SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*), 2) as uptime_percent
FROM runs
WHERE site_id = 1 
AND started_at > NOW() - INTERVAL '7 days';

-- Top failures
SELECT f.type, f.message, COUNT(*) as count
FROM failures f
JOIN runs r ON r.id = f.run_id
WHERE r.site_id = 1
AND r.started_at > NOW() - INTERVAL '7 days'
GROUP BY f.type, f.message
ORDER BY count DESC
LIMIT 5;

-- P95 performance
SELECT 
  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY load_ms) as p95_load,
  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY ttfb_ms) as p95_ttfb
FROM page_results pr
JOIN runs r ON r.id = pr.run_id
WHERE r.site_id = 1
AND r.started_at > NOW() - INTERVAL '7 days';
```

---

## Environment Variables

### Development (.env file)
```bash
# Create .env in project root
cat > .env << EOF
# PostgreSQL
POSTGRES_USER=monitor
POSTGRES_PASSWORD=monitor123
POSTGRES_DB=monitoring

# RabbitMQ
RABBITMQ_USER=monitor
RABBITMQ_PASSWORD=monitor123

# SMTP (optional)
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
EOF

# Load in terminal
export $(cat .env | xargs)
```

### Production
```bash
# Override in application.yml via env vars
export SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/monitoring
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=secure_password

export SPRING_RABBITMQ_HOST=prod-rabbitmq
export SPRING_RABBITMQ_USERNAME=prod_user
export SPRING_RABBITMQ_PASSWORD=secure_password

export MONITORING_EMAIL_ENABLED=true
export SMTP_USERNAME=notifications@company.com
export SMTP_PASSWORD=app_password
```

---

## Shortcuts

### One-liner Full Reset
```bash
docker-compose down -v && docker-compose up -d && sleep 20 && cd monitor-api && mvn spring-boot:run &
```

### Watch Logs
```bash
# All errors
tail -f monitor-api/target/*.log monitor-runner/target/*.log | grep ERROR

# Specific component
tail -f monitor-api/target/*.log | grep "Quartz\|CheckScheduler\|Ingest\|SSE"
```

### Count Records
```bash
echo "SELECT 
  (SELECT COUNT(*) FROM sites) as sites,
  (SELECT COUNT(*) FROM site_pages) as pages,
  (SELECT COUNT(*) FROM runs) as runs,
  (SELECT COUNT(*) FROM failures) as failures;" \
| psql -h localhost -U monitor -d monitoring
```

---

**Happy Coding! 🚀**
