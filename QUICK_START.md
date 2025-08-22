# 🚀 Início Rápido - 5 Minutos

## Para Usuários Windows (Sem Maven)

1. **Abra PowerShell como Administrador**
2. **Execute o comando:**
   ```powershell
   .\scripts\setup-docker-only.ps1
   ```
3. **Escolha opção 1** (Full setup)
4. **Aguarde 2-3 minutos**
5. **Acesse:** http://localhost:3000

## Para Usuários com Maven

1. **Execute:**
   ```powershell
   .\scripts\setup.ps1
   ```
2. **Escolha opção 1**
3. **Aguarde o build**
4. **Acesse:** http://localhost:3000

## Verificar se Funcionou

✅ **Frontend:** http://localhost:3000  
✅ **API:** http://localhost:8080/health  
✅ **RabbitMQ:** http://localhost:15672 (guest/guest)

## Testar o Sistema

1. **Vá para Pedidos:** http://localhost:3000/orders
2. **Clique "Novo Pedido"**
3. **Preencha:**
   - Cliente: `customer-123`
   - Produto: `product-001`
   - Nome: `Produto Teste`
   - Quantidade: `2`
   - Preço: `50.00`
4. **Clique "Criar Pedido"**
5. **Veja o pedido sendo processado!**

## Parar o Sistema

```powershell
docker-compose down
```

## Problemas?

- **Docker não instalado?** → https://www.docker.com/products/docker-desktop/
- **Erro de porta?** → Feche outros programas usando as portas 3000, 8080
- **Erro de memória?** → Aumente memória do Docker Desktop para 4GB+

## Logs

```powershell
# Ver todos os logs
docker-compose logs -f

# Ver logs de um serviço
docker-compose logs -f order-service
```

---

**🎉 Pronto! Seu sistema está rodando!**