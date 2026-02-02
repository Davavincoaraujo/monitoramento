# 🚀 Deploy Gratuito no Render.com

Guia completo para colocar o sistema de monitoramento no ar **gratuitamente** e **24/7**.

## 🎯 Por que Render.com?

- ✅ **Grátis para sempre** (não é trial, é free tier permanente)
- ✅ **750 horas/mês grátis** por serviço (suficiente para 24/7)
- ✅ **PostgreSQL gratuito** incluso (1GB storage)
- ✅ **SSL/HTTPS automático** (certificado grátis)
- ✅ **Deploy via Git push** (CI/CD automático)
- ✅ **Sem cartão de crédito necessário**
- ✅ **URL pública**: `https://monitor-api.onrender.com`

## 📋 Pré-requisitos

1. Conta no GitHub (já tem ✅)
2. Criar conta gratuita no [Render.com](https://render.com)

## 🚀 Passo a Passo

### 1. Conectar GitHub ao Render

1. Acesse [https://render.com](https://render.com)
2. Clique em **"Get Started for Free"**
3. Faça login com GitHub
4. Autorize Render a acessar seus repositórios

### 2. Criar Blueprint

1. No dashboard do Render, clique em **"New +"**
2. Selecione **"Blueprint"**
3. Conecte ao repositório: `Davavincoaraujo/monitoramento`
4. Render detectará automaticamente o arquivo `render.yaml`
5. Clique em **"Apply"**

**Render irá criar automaticamente:**
- ✅ PostgreSQL database
- ✅ monitor-api (Web Service)
- ✅ monitor-runner (Background Worker)

### 3. Aguardar Deploy (5-10 minutos)

Render irá:
1. Build das imagens Docker
2. Instalar Playwright browsers
3. Aplicar migrations Flyway
4. Iniciar serviços

**Acompanhe no dashboard:**
- Logs em tempo real
- Status de cada serviço
- URL pública gerada

### 4. Acessar Aplicação

Após deploy completo:

```bash
# URL pública da API
https://monitor-api.onrender.com

# Endpoints disponíveis
https://monitor-api.onrender.com/actuator/health
https://monitor-api.onrender.com/api/sites
https://monitor-api.onrender.com/api/dashboard/overview?siteId=1&range=24h
```

## 🔧 Configuração Pós-Deploy

### Criar Site de Teste

```bash
# Via cURL
curl -X POST https://monitor-api.onrender.com/api/sites \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Google",
    "baseUrl": "https://www.google.com",
    "enabled": true,
    "frequencySeconds": 300,
    "pages": [
      {"name": "Home", "path": "/", "enabled": true}
    ]
  }'
```

### Verificar Monitoramento

```bash
# Health check
curl https://monitor-api.onrender.com/actuator/health

# Listar sites
curl https://monitor-api.onrender.com/api/sites

# Ver runs
curl "https://monitor-api.onrender.com/api/runs?siteId=1&from=$(date -u -v-1d +%Y-%m-%dT%H:%M:%S)&to=$(date -u +%Y-%m-%dT%H:%M:%S)"
```

## ⚙️ Configurações do Free Tier

### Limites Gratuitos

| Recurso | Limite Free Tier |
|---------|------------------|
| Web Services | 750h/mês (1 serviço 24/7) |
| Workers | 750h/mês (1 worker 24/7) |
| PostgreSQL | 1GB storage, 1 database |
| RAM por serviço | 512MB |
| CPU | Compartilhado |
| Build time | 500 horas/mês |
| Bandwidth | 100GB/mês |

### Comportamento do Free Tier

**Web Service (monitor-api):**
- Dorme após **15 minutos** de inatividade
- Acorda automaticamente no primeiro request (cold start ~30s)
- Para amostra/demo é perfeito

**Worker (monitor-runner):**
- Roda continuamente se tiver mensagens RabbitMQ
- Pode dormir se ficar idle

**PostgreSQL:**
- Sempre ativo
- Não dorme
- Backups automáticos

## 🎨 Melhorias Recomendadas

### 1. Adicionar Cron Job (Manter API Acordada)

No Render dashboard:
1. Criar novo **"Cron Job"** (free)
2. Command: `curl https://monitor-api.onrender.com/actuator/health`
3. Schedule: `*/10 * * * *` (a cada 10 minutos)

Isso evita cold starts frequentes.

### 2. Monitoramento Externo

Adicionar site próprio ao monitoramento:
```json
{
  "name": "Monitor API",
  "baseUrl": "https://monitor-api.onrender.com",
  "pages": [{"name": "Health", "path": "/actuator/health"}]
}
```

### 3. Variáveis de Ambiente

No Render dashboard, adicionar:
```
MONITORING_DEFAULT_CHECK_FREQUENCY_SECONDS=600
MONITORING_EMAIL_ENABLED=false
SPRING_PROFILES_ACTIVE=production
```

## 📊 Alternativas Gratuitas

Se precisar de mais recursos:

### Railway.app
- **Free**: $5 crédito/mês (suficiente para hobby)
- PostgreSQL incluso
- Deploy via Git
- URL: `https://monitoramento.up.railway.app`

### Fly.io
- **Free**: 3 VMs + PostgreSQL
- Mais complexo, mas mais recursos
- Global edge network

### Oracle Cloud Always Free
- **Free**: 2 VMs, 200GB storage, PostgreSQL
- Mais complexo de configurar
- Never expires

## 🔒 Segurança (Produção)

Para uso real (não demo):

1. **Adicionar autenticação**:
```java
// Spring Security
implementation 'org.springframework.boot:spring-boot-starter-security'
```

2. **Variáveis sensíveis no Render**:
- Database password (auto-gerado)
- SMTP credentials
- API keys

3. **Rate limiting** (já implementado ✅)

4. **CORS configurado**:
```yaml
cors:
  allowed-origins: https://seu-frontend.com
```

## 💰 Upgrade Opcional

Se projeto crescer, planos pagos:

| Plano | Preço | Benefícios |
|-------|-------|------------|
| Starter | $7/mês | Sem sleep, SSL custom |
| Standard | $25/mês | 4GB RAM, mais CPU |
| Pro | $85/mês | 16GB RAM, SLA 99.95% |

## 🐛 Troubleshooting

### Build falhou
```bash
# Ver logs no dashboard
# Comum: falta memória (max 512MB no free)
# Solução: otimizar Dockerfile
```

### Cold start lento
```bash
# Normal no free tier (até 30s)
# Solução: usar cron job para manter acordado
```

### Playwright não funciona
```bash
# Verificar instalação de dependências no Dockerfile
# Render usa Ubuntu 20.04
# Firefox é mais leve que Chromium
```

### Database connection error
```bash
# Verificar env vars no dashboard
# SPRING_DATASOURCE_URL deve apontar para postgres interno
```

## 📱 Próximos Passos

1. **Frontend React** (deploy no Vercel/Netlify grátis):
```bash
npx create-react-app monitor-dashboard
# Conectar à API: https://monitor-api.onrender.com
```

2. **Custom Domain** (Render Pro):
```
monitor.seudominio.com
```

3. **CI/CD Automático**:
```bash
git push origin main
# Render detecta e faz deploy automaticamente
```

## 🎯 Demonstração/Portfólio

URL para compartilhar:
```
🌐 Site: https://monitor-api.onrender.com
📊 Health: https://monitor-api.onrender.com/actuator/health
📈 API Docs: https://monitor-api.onrender.com/swagger-ui.html (se adicionar)
💻 GitHub: https://github.com/Davavincoaraujo/monitoramento
```

**No README.md do GitHub:**
```markdown
## 🌐 Demo Online
- API: https://monitor-api.onrender.com
- Docs: [Ver documentação](./ARCHITECTURE.md)
```

---

**Resumo:** Com Render.com você tem um sistema **profissional**, **gratuito** e **sempre online** para demonstrações, portfólio ou uso pessoal. Para produção com muitos usuários, considere upgrade ou AWS/Azure.
