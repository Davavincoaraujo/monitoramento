# Arquivos Kubernetes para Deploy

Este diretório contém os manifestos Kubernetes para deploy do sistema de monitoramento.

## 📦 Estrutura

```
kubernetes/
├── namespace.yaml      # Namespace "monitoring"
├── configmap.yaml      # ConfigMaps para API e Runner
├── deployment.yaml     # Deployments com health probes
└── service.yaml        # Services (LoadBalancer + ClusterIP)
```

## 🚀 Deploy

### 1. Criar Namespace

```bash
kubectl apply -f namespace.yaml
```

### 2. Aplicar ConfigMaps

```bash
kubectl apply -f configmap.yaml
```

### 3. Deploy Aplicações

```bash
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
```

### 4. Verificar Status

```bash
# Pods
kubectl get pods -n monitoring

# Services
kubectl get svc -n monitoring

# Logs
kubectl logs -f deployment/monitor-api -n monitoring
kubectl logs -f deployment/monitor-runner -n monitoring
```

## 🏥 Health Checks

### Liveness Probes

- **monitor-api**: `GET /actuator/health/liveness` (porta 8080)
  - Initial Delay: 60s
  - Period: 10s
  - Timeout: 5s
  - Failure Threshold: 3

- **monitor-runner**: `GET /actuator/health/liveness` (porta 8081)
  - Initial Delay: 90s (mais tempo para Playwright)
  - Period: 15s
  - Timeout: 5s
  - Failure Threshold: 3

### Readiness Probes

- **monitor-api**: `GET /actuator/health/readiness` (porta 8080)
  - Initial Delay: 30s
  - Period: 5s
  - Timeout: 3s
  - Failure Threshold: 3

- **monitor-runner**: `GET /actuator/health/readiness` (porta 8081)
  - Initial Delay: 45s
  - Period: 10s
  - Timeout: 5s
  - Failure Threshold: 3

## 📊 Resources

### monitor-api (2 réplicas)

```yaml
requests:
  memory: 512Mi
  cpu: 250m
limits:
  memory: 1Gi
  cpu: 1000m
```

### monitor-runner (3 réplicas)

```yaml
requests:
  memory: 1Gi     # Maior por causa do Playwright
  cpu: 500m
limits:
  memory: 2Gi
  cpu: 2000m
```

## 🔧 Configuração

### Variáveis de Ambiente

```yaml
JAVA_OPTS: "-Xmx512m -Xms256m"  # API
JAVA_OPTS: "-Xmx768m -Xms384m"  # Runner
SPRING_PROFILES_ACTIVE: "production"
```

### ConfigMaps

- `monitor-api-config`: Configuração Spring Boot para API
- `monitor-runner-config`: Configuração Spring Boot para Runner

## 🌐 Serviços

### monitor-api-service (LoadBalancer)

- **Tipo**: LoadBalancer
- **Porta**: 8080
- **Acesso Externo**: Sim
- **Endpoints**:
  - API REST: `http://<EXTERNAL-IP>:8080/api`
  - Health: `http://<EXTERNAL-IP>:8080/actuator/health`
  - Metrics: `http://<EXTERNAL-IP>:8080/actuator/metrics`

### monitor-runner-service (ClusterIP)

- **Tipo**: ClusterIP (interno apenas)
- **Porta**: 8081
- **Acesso Externo**: Não

## 📝 Dependências Externas

Este manifesto assume que você tem:

- **PostgreSQL**: Service `postgres-service:5432`
- **RabbitMQ**: Service `rabbitmq-service:5672`

Você precisa criar estes services separadamente ou usar Helm charts:

```bash
# Exemplo com Helm
helm install postgres bitnami/postgresql --namespace monitoring
helm install rabbitmq bitnami/rabbitmq --namespace monitoring
```

## 🔍 Troubleshooting

### Pod não inicia (CrashLoopBackOff)

```bash
# Ver logs
kubectl logs <pod-name> -n monitoring

# Descrever pod
kubectl describe pod <pod-name> -n monitoring

# Verificar probes
kubectl get pod <pod-name> -n monitoring -o yaml | grep -A 10 "livenessProbe"
```

### Readiness probe falhando

```bash
# Testar probe manualmente
kubectl exec -it <pod-name> -n monitoring -- curl http://localhost:8080/actuator/health/readiness

# Verificar logs de health checks
kubectl logs <pod-name> -n monitoring | grep -i health
```

### Alta utilização de recursos

```bash
# Ver uso de CPU/Memory
kubectl top pods -n monitoring

# Escalar deployment
kubectl scale deployment monitor-api --replicas=3 -n monitoring
```

## 🎯 Acessar Aplicação

### Obter IP Externo

```bash
kubectl get svc monitor-api-service -n monitoring

# Output exemplo:
# NAME                  TYPE           EXTERNAL-IP     PORT(S)
# monitor-api-service   LoadBalancer   35.123.45.67    8080:30123/TCP
```

### Testar API

```bash
# Health check
curl http://<EXTERNAL-IP>:8080/actuator/health

# Listar sites
curl http://<EXTERNAL-IP>:8080/api/sites

# Dashboard
curl "http://<EXTERNAL-IP>:8080/api/dashboard/overview?siteId=1&range=24h"
```

## 📈 Monitoramento

### Prometheus Scraping

Endpoints disponíveis para scraping:

```yaml
- job_name: 'monitor-api'
  kubernetes_sd_configs:
    - role: pod
      namespaces:
        names:
          - monitoring
  relabel_configs:
    - source_labels: [__meta_kubernetes_pod_label_app]
      regex: monitor-api
      action: keep
  metrics_path: /actuator/prometheus

- job_name: 'monitor-runner'
  kubernetes_sd_configs:
    - role: pod
      namespaces:
        names:
          - monitoring
  relabel_configs:
    - source_labels: [__meta_kubernetes_pod_label_app]
      regex: monitor-runner
      action: keep
  metrics_path: /actuator/prometheus
```

## 🔄 Update/Rollback

### Atualizar imagem

```bash
kubectl set image deployment/monitor-api monitor-api=monitor-api:1.1.0 -n monitoring
kubectl set image deployment/monitor-runner monitor-runner=monitor-runner:1.1.0 -n monitoring
```

### Rollback

```bash
# Ver histórico
kubectl rollout history deployment/monitor-api -n monitoring

# Rollback
kubectl rollout undo deployment/monitor-api -n monitoring
```

### Rolling Update

Deployments estão configurados com estratégia RollingUpdate padrão:
- MaxUnavailable: 25%
- MaxSurge: 25%

Zero downtime durante updates.
