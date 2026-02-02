# 📡 API REST - Documentação Completa

## Base URL
```
http://localhost:8080/api
```

## Autenticação
Atualmente **não há autenticação**. Todas as requisições são públicas.

---

## 🌐 Sites

### Listar Sites
```http
GET /api/sites
```

**Response 200:**
```json
[
  {
    "id": 1,
    "name": "Example Site",
    "baseUrl": "https://example.com",
    "checkIntervalMinutes": 5,
    "isActive": true,
    "createdAt": "2026-02-01T10:00:00"
  }
]
```

### Buscar Site
```http
GET /api/sites/{id}
```

**Response 200:**
```json
{
  "id": 1,
  "name": "Example Site",
  "baseUrl": "https://example.com",
  "checkIntervalMinutes": 5,
  "isActive": true,
  "pages": [
    {
      "id": 1,
      "name": "Home",
      "path": "/",
      "isActive": true
    }
  ]
}
```

### Criar Site
```http
POST /api/sites
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Meu Site",
  "baseUrl": "https://example.com",
  "checkIntervalMinutes": 5
}
```

**Response 201:**
```json
{
  "id": 2,
  "name": "Meu Site",
  "baseUrl": "https://example.com",
  "checkIntervalMinutes": 5,
  "isActive": true,
  "createdAt": "2026-02-02T10:30:00"
}
```

### Atualizar Site
```http
PUT /api/sites/{id}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Meu Site Atualizado",
  "checkIntervalMinutes": 10,
  "isActive": true
}
```

**Response 200:** (mesmo formato do GET)

### Deletar Site
```http
DELETE /api/sites/{id}
```

**Response 204:** No Content

### Executar Check Manual
```http
POST /api/sites/{id}/check
```

**Response 202:** Accepted
```json
{
  "message": "Check solicitado. Aguarde processamento.",
  "siteId": 1
}
```

---

## 📊 Dashboard

### Overview
```http
GET /api/dashboard/overview?siteId={id}&range={range}
```

**Query Parameters:**
- `siteId` (required): ID do site
- `range` (optional): `1h`, `6h`, `24h` (default), `7d`, `30d`

**Response 200:**
```json
{
  "siteId": 1,
  "siteName": "Example Site",
  "status": "UP",
  "uptimePercent": 99.5,
  "issuesBySeverity": {
    "CRITICAL": 2,
    "MAJOR": 5,
    "MINOR": 10
  },
  "performance": {
    "p50LoadMs": 1200,
    "p95LoadMs": 3500,
    "p99LoadMs": 5000,
    "p95TtfbMs": 800
  },
  "lastRun": {
    "runId": 123,
    "startedAt": "2026-02-02T10:30:00",
    "status": "SUCCESS",
    "criticalCount": 0,
    "majorCount": 1,
    "minorCount": 2
  }
}
```

### Timeseries - Erros
```http
GET /api/dashboard/timeseries/errors?siteId={id}&range={range}&bucket={bucket}
```

**Query Parameters:**
- `siteId` (required): ID do site
- `range` (optional): `1h`, `6h`, `24h` (default), `7d`
- `bucket` (optional): `5m`, `1h` (default), `6h`, `1d`

**Response 200:**
```json
{
  "metric": "errors",
  "dataPoints": [
    {
      "timestamp": "2026-02-02T09:00:00",
      "value": 3
    },
    {
      "timestamp": "2026-02-02T10:00:00",
      "value": 5
    }
  ]
}
```

### Timeseries - Performance
```http
GET /api/dashboard/timeseries/perf?siteId={id}&range={range}&bucket={bucket}
```

**Query Parameters:**
- `siteId` (required): ID do site
- `range` (optional): `24h`, `7d` (default), `30d`
- `bucket` (optional): `1h`, `6h` (default), `1d`

**Response 200:**
```json
{
  "metric": "performance",
  "dataPoints": [
    {
      "timestamp": "2026-02-01T12:00:00",
      "value": 1850
    },
    {
      "timestamp": "2026-02-01T18:00:00",
      "value": 2100
    }
  ]
}
```

---

## 🏃 Runs (Execuções)

### Listar Runs
```http
GET /api/runs?siteId={id}&from={datetime}&to={datetime}
```

**Query Parameters:**
- `siteId` (required): ID do site
- `from` (required): ISO 8601 datetime (e.g., `2026-02-01T00:00:00`)
- `to` (required): ISO 8601 datetime

**Response 200:**
```json
[
  {
    "id": 123,
    "siteId": 1,
    "startedAt": "2026-02-02T10:30:00",
    "endedAt": "2026-02-02T10:30:15",
    "status": "FAILED",
    "criticalCount": 2,
    "majorCount": 1,
    "minorCount": 0,
    "summary": "Completed 3 pages, 2 critical, 1 major issues",
    "failures": [
      {
        "id": 456,
        "severity": "CRITICAL",
        "type": "REQUEST_FAILED",
        "message": "Request failed: NS_BINDING_ABORTED",
        "url": "https://example.com/api/data",
        "pageName": "Home"
      }
    ]
  }
]
```

### Detalhes de Run
```http
GET /api/runs/{id}
```

**Response 200:**
```json
{
  "id": 123,
  "site": {
    "id": 1,
    "name": "Example Site"
  },
  "startedAt": "2026-02-02T10:30:00",
  "endedAt": "2026-02-02T10:30:15",
  "status": "FAILED",
  "criticalCount": 2,
  "majorCount": 1,
  "minorCount": 0,
  "summary": "Completed 3 pages, 2 critical, 1 major issues",
  "pageResults": [
    {
      "id": 789,
      "page": {
        "id": 1,
        "name": "Home",
        "path": "/"
      },
      "ttfbMs": 250,
      "loadTimeMs": 1850,
      "domContentMs": 1200,
      "requestCount": 42,
      "failedRequestCount": 2,
      "jsErrorCount": 0,
      "consoleErrorCount": 1,
      "status": "FAILED"
    }
  ],
  "failures": [
    {
      "id": 456,
      "page": {
        "id": 1,
        "name": "Home"
      },
      "severity": "CRITICAL",
      "type": "REQUEST_FAILED",
      "message": "Request failed: NS_BINDING_ABORTED",
      "url": "https://example.com/api/data",
      "createdAt": "2026-02-02T10:30:12"
    }
  ],
  "requestErrors": [
    {
      "id": 321,
      "page": {
        "id": 1,
        "name": "Home"
      },
      "url": "https://example.com/missing.png",
      "method": "GET",
      "statusCode": 404,
      "errorText": "Not Found",
      "createdAt": "2026-02-02T10:30:10"
    }
  ]
}
```

---

## 🔴 Live Monitoring (SSE)

### Stream ao Vivo
```http
GET /api/live?url={url}
Accept: text/event-stream
```

**Query Parameters:**
- `url` (required): URL completa para monitorar (e.g., `https://example.com`)

**Response 200:** Server-Sent Events

**Event Types:**

#### 1. Status Update
```
event: status
data: {"status":"running","message":"Iniciando navegação..."}
```

#### 2. Progress Update
```
event: progress
data: {"status":"processing","message":"Coletando métricas..."}
```

#### 3. Completed
```
event: completed
data: {
  "status": "completed",
  "result": {
    "runId": 124,
    "status": "SUCCESS",
    "pageResults": [{
      "pageName": "Example Page",
      "ttfbMs": 200,
      "loadTimeMs": 1500,
      "requestCount": 35
    }],
    "failures": []
  }
}
```

#### 4. Error
```
event: error
data: {"status":"error","message":"Erro ao navegar: Timeout"}
```

**JavaScript Client Example:**
```javascript
const eventSource = new EventSource('/api/live?url=https://example.com');

eventSource.addEventListener('status', (e) => {
    const data = JSON.parse(e.data);
    console.log('Status:', data.message);
});

eventSource.addEventListener('completed', (e) => {
    const data = JSON.parse(e.data);
    console.log('Resultado:', data.result);
    eventSource.close();
});

eventSource.addEventListener('error', (e) => {
    const data = JSON.parse(e.data);
    console.error('Erro:', data.message);
    eventSource.close();
});
```

---

## 📋 Enums e Códigos

### RunStatus
```
RUNNING    - Execução em andamento
SUCCESS    - Concluído com sucesso (sem critical/major issues)
WARNING    - Concluído com warnings (minor issues apenas)
FAILED     - Concluído com falhas (critical ou major issues)
ERROR      - Erro na execução (timeout, crash, etc)
```

### Severity
```
CRITICAL   - Impacto alto, ação imediata necessária
MAJOR      - Impacto médio, deve ser resolvido em breve
MINOR      - Impacto baixo, pode ser resolvido depois
```

### FailureType
```
REQUEST_FAILED      - Request HTTP falhou (timeout, abort, network)
JS_ERROR           - Erro JavaScript capturado
NAVIGATION_FAILED  - Falha ao navegar para página
CONSOLE_ERROR      - Erro de console do browser
TIMEOUT            - Timeout na execução
```

---

## ⚠️ Códigos de Erro

### 400 Bad Request
```json
{
  "error": "Bad Request",
  "message": "URL inválida: deve começar com http:// ou https://",
  "timestamp": "2026-02-02T10:30:00"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Site não encontrado",
  "timestamp": "2026-02-02T10:30:00"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "Erro ao processar requisição",
  "timestamp": "2026-02-02T10:30:00"
}
```

---

## 🔧 Rate Limiting

**Atualmente não implementado.**

Recomendações:
- Máximo 10 requests/segundo por IP
- Máximo 1 live monitoring concorrente por IP

---

## 📝 Notas

1. **Timestamps:** Todos em ISO 8601 format (UTC ou local timezone)
2. **IDs:** Sempre `Long` (números inteiros positivos)
3. **URLs:** Sempre completas (incluindo protocolo)
4. **Paginação:** Não implementada (retorna todos os resultados)
5. **Ordenação:** Sempre descendente por `created_at` ou `started_at`

---

## 🧪 Exemplos com cURL

### Criar site e executar check
```bash
# Criar
curl -X POST http://localhost:8080/api/sites \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Site",
    "baseUrl": "https://example.com",
    "checkIntervalMinutes": 5
  }'

# Executar check
curl -X POST http://localhost:8080/api/sites/1/check

# Ver resultado (aguarde ~15s)
curl "http://localhost:8080/api/runs?siteId=1&from=2026-02-02T00:00:00&to=2026-02-02T23:59:59"
```

### Dashboard
```bash
# Overview
curl "http://localhost:8080/api/dashboard/overview?siteId=1&range=24h" | jq

# Performance
curl "http://localhost:8080/api/dashboard/timeseries/perf?siteId=1&range=7d&bucket=6h" | jq
```

### Live Monitoring com curl
```bash
curl -N "http://localhost:8080/api/live?url=https://example.com"
# -N: Disable buffering para ver eventos em tempo real
```

---

**Versão:** 1.0  
**Última atualização:** 02/02/2026
