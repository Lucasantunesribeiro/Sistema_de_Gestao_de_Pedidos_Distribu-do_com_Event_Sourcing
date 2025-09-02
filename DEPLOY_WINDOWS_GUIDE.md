# Guia de Deploy para Windows - Sistema Unificado

## 🎯 Deploy usando Infraestrutura Existente no Windows

### 📋 Pré-requisitos
- Windows PowerShell ou Command Prompt
- Git instalado
- Curl instalado (ou usar PowerShell equivalente)
- Acesso ao Render.com Dashboard

### 🚀 Passo a Passo

#### 1. **Preparar o Deploy**
```cmd
# Execute o script de preparação
prepare-existing-deploy.bat
```

#### 2. **Commit e Push (PowerShell)**
```powershell
git add .
git commit -m "feat: deploy sistema unificado para infraestrutura existente"
git push origin main
```

#### 3. **Configurar Web Service no Render.com**

Acesse seu Web Service `Gestao_de_Pedidos` e atualize:

**Build & Deploy:**
- **Root Directory**: `unified-order-system`
- **Build Command**: `./mvnw clean package -DskipTests`
- **Start Command**: 
  ```
  java -Dspring.profiles.active=render -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -jar target/unified-order-system-1.0.0.jar
  ```

**Environment Variables:**
```
SPRING_PROFILES_ACTIVE=render
SERVER_PORT=8080
JPA_DDL_AUTO=update
JPA_SHOW_SQL=false
```

**Health Check:**
- **Path**: `/actuator/health`

#### 4. **Fazer Deploy Manual**
1. No Render Dashboard → `Gestao_de_Pedidos`
2. Clique em "Manual Deploy"
3. Selecione branch `main`
4. Clique em "Deploy"

#### 5. **Testar o Deploy**
```cmd
# Execute o script de teste
test-infrastructure.bat
```

### 🔧 Comandos Alternativos para Windows

#### Testar Health Check (PowerShell)
```powershell
$response = Invoke-RestMethod -Uri "https://gestao-de-pedidos.onrender.com/actuator/health" -Method Get
$response | ConvertTo-Json -Depth 3
```

#### Testar API de Pedidos (PowerShell)
```powershell
# Listar pedidos
$orders = Invoke-RestMethod -Uri "https://gestao-de-pedidos.onrender.com/api/orders" -Method Get
$orders

# Criar pedido de teste
$body = @{
    customerId = "test-customer-001"
    items = @(
        @{
            productId = "product-1"
            quantity = 2
            price = 99.99
        }
    )
} | ConvertTo-Json -Depth 3

$newOrder = Invoke-RestMethod -Uri "https://gestao-de-pedidos.onrender.com/api/orders" -Method Post -Body $body -ContentType "application/json"
$newOrder
```

#### Verificar Logs (PowerShell)
```powershell
# Monitorar health check continuamente
while ($true) {
    try {
        $health = Invoke-RestMethod -Uri "https://gestao-de-pedidos.onrender.com/actuator/health" -Method Get
        Write-Host "$(Get-Date): Status = $($health.status)" -ForegroundColor Green
        Start-Sleep -Seconds 30
    }
    catch {
        Write-Host "$(Get-Date): Health check failed" -ForegroundColor Red
        Start-Sleep -Seconds 30
    }
}
```

### 🛠️ Troubleshooting Windows

#### Problema: Curl não encontrado
**Solução 1 - Instalar Curl:**
```powershell
# Via Chocolatey
choco install curl

# Via Scoop
scoop install curl
```

**Solução 2 - Usar PowerShell:**
```powershell
# Substituir curl por Invoke-RestMethod
Invoke-RestMethod -Uri "https://gestao-de-pedidos.onrender.com/actuator/health"
```

#### Problema: Maven não encontrado
**Solução:**
```cmd
# Verificar se Java está instalado
java -version

# Usar Maven wrapper do projeto
cd unified-order-system
mvnw.cmd --version
```

#### Problema: Git Bash vs PowerShell
**Solução:**
- Use **PowerShell** para comandos Windows nativos
- Use **Git Bash** para comandos Unix-like
- Scripts `.bat` funcionam em ambos

### 📊 Monitoramento Contínuo (Windows)

#### Script de Monitoramento (PowerShell)
```powershell
# Salvar como monitor-deploy.ps1
param(
    [string]$BaseUrl = "https://gestao-de-pedidos.onrender.com",
    [int]$IntervalSeconds = 60
)

Write-Host "🔍 Monitorando deploy em $BaseUrl" -ForegroundColor Cyan

while ($true) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    
    try {
        # Health Check
        $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
        $status = $health.status
        
        if ($status -eq "UP") {
            Write-Host "[$timestamp] ✅ Sistema UP - Database: $($health.components.database.status) | Redis: $($health.components.redis.status)" -ForegroundColor Green
        } else {
            Write-Host "[$timestamp] ⚠️  Sistema: $status" -ForegroundColor Yellow
        }
        
        # Test API
        $orders = Invoke-RestMethod -Uri "$BaseUrl/api/orders" -TimeoutSec 10
        Write-Host "[$timestamp] 📊 API Orders: $($orders.Count) pedidos" -ForegroundColor Blue
        
    }
    catch {
        Write-Host "[$timestamp] ❌ Erro: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds $IntervalSeconds
}
```

#### Executar Monitoramento:
```powershell
# Executar o script
.\monitor-deploy.ps1

# Ou com parâmetros customizados
.\monitor-deploy.ps1 -BaseUrl "https://gestao-de-pedidos.onrender.com" -IntervalSeconds 30
```

### 🎯 Checklist Final

#### ✅ Antes do Deploy:
- [ ] Código commitado e pushed
- [ ] Build local funcionando (`mvnw.cmd clean package`)
- [ ] Testes passando (`mvnw.cmd test`)
- [ ] Configurações do Render atualizadas

#### ✅ Após o Deploy:
- [ ] Health check retornando UP
- [ ] Database conectado
- [ ] Redis conectado
- [ ] APIs respondendo
- [ ] Swagger UI acessível

#### ✅ Monitoramento:
- [ ] Logs do Render sem erros
- [ ] Métricas de performance normais
- [ ] Endpoints respondendo < 500ms
- [ ] Sem memory leaks

### 🔗 URLs Importantes

- **Dashboard**: https://dashboard.render.com
- **Aplicação**: https://gestao-de-pedidos.onrender.com
- **Health**: https://gestao-de-pedidos.onrender.com/actuator/health
- **API Docs**: https://gestao-de-pedidos.onrender.com/swagger-ui.html
- **Orders**: https://gestao-de-pedidos.onrender.com/api/orders
- **Query**: https://gestao-de-pedidos.onrender.com/api/query

### 💡 Dicas Windows

1. **Use PowerShell ISE** para scripts mais complexos
2. **Git Bash** para comandos Unix familiares
3. **Windows Terminal** para melhor experiência
4. **Chocolatey** para instalar ferramentas facilmente
5. **WSL2** se preferir ambiente Linux

---

**🎉 Deploy concluído! Seu sistema unificado está rodando na infraestrutura existente do Render.com!**