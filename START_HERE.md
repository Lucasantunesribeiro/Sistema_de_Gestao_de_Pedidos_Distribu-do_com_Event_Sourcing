# 🚀 COMO INICIAR O SISTEMA

## ⚡ Início Rápido (1 comando)

```powershell
.\start-system.ps1
```

**Aguarde 2-3 minutos e acesse:** http://localhost:3000

---

## 📋 Pré-requisitos

✅ **Docker Desktop** instalado e rodando  
📥 Download: https://www.docker.com/products/docker-desktop/

---

## 🎯 O que o sistema faz

- **Dashboard** com métricas em tempo real
- **Gestão de Pedidos** completa
- **Controle de Pagamentos**
- **Gerenciamento de Estoque**
- **Arquitetura de Microsserviços**

---

## 🌐 URLs do Sistema

| Serviço | URL | Descrição |
|---------|-----|-----------|
| **Frontend** | http://localhost:3000 | Interface principal |
| **API Gateway** | http://localhost:8080 | APIs dos microsserviços |
| **RabbitMQ** | http://localhost:15672 | Message broker (guest/guest) |

---

## 🛠️ Comandos Úteis

```powershell
# Iniciar sistema
.\start-system.ps1

# Ver logs em tempo real
docker-compose logs -f

# Parar sistema
docker-compose down

# Ver status dos serviços
docker-compose ps

# Reiniciar um serviço específico
docker-compose restart order-service

# Limpar tudo (cuidado: apaga dados)
docker-compose down -v
```

---

## 🧪 Testando o Sistema

1. **Acesse:** http://localhost:3000
2. **Vá para "Pedidos"**
3. **Clique "Novo Pedido"** (se disponível)
4. **Explore o Dashboard**
5. **Verifique Pagamentos e Estoque**

---

## 🆘 Problemas Comuns

### ❌ "Docker não está rodando"
**Solução:** Inicie o Docker Desktop

### ❌ "Porta já em uso"
**Solução:** 
```powershell
# Parar outros serviços
docker-compose down

# Verificar portas em uso
netstat -an | findstr :3000
netstat -an | findstr :8080
```

### ❌ "Serviços não respondem"
**Solução:**
```powershell
# Aguardar mais tempo
Start-Sleep -Seconds 60

# Verificar logs
docker-compose logs order-service
docker-compose logs frontend
```

### ❌ "Erro de memória"
**Solução:** Aumente a memória do Docker Desktop para 4GB+

---

## 📊 Arquitetura

```
Frontend (React) → Nginx Proxy → Microsserviços
                                      ↓
                              PostgreSQL + RabbitMQ + Redis
```

**Microsserviços:**
- **Order Service** (8081) - Gestão de pedidos
- **Payment Service** (8082) - Processamento de pagamentos  
- **Inventory Service** (8083) - Controle de estoque
- **Query Service** (8084) - Consultas otimizadas

---

## 🎉 Pronto!

Seu sistema de gestão de pedidos distribuído está rodando!

**Próximos passos:**
- Explore a interface em http://localhost:3000
- Teste criar pedidos
- Monitore os logs
- Customize conforme necessário

---

**💡 Dica:** Mantenha o Docker Desktop rodando sempre que usar o sistema!