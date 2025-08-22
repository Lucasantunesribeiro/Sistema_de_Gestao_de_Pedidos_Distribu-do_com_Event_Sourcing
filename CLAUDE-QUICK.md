# Sistema de Gestão de Pedidos - Setup Rápido

## Arquitetura
- **Order Service** (8081): Event Sourcing + Commands
- **Payment Service** (8082): Payment processing  
- **Inventory Service** (8083): Stock management
- **Query Service** (8084): CQRS Read Models
- **Frontend** (3000): React + shadcn/ui

## Comandos Essenciais
- `pedidos-quick dev`: Start development environment
- `pedidos backend`: Start all microservices
- `pedidos frontend`: Start React frontend
- `claude code .`: Start Claude Code with MCPs

## MCPs Configurados
- 📁 **filesystem**: Project navigation
- 📦 **maven**: Java dependencies
- 🗄️ **postgres**: Database management
- 🐳 **docker**: Container orchestration  
- 🎨 **shadcn-ui**: React components

## Quick Start
1. `pedidos-quick dev` - Start infrastructure
2. `claude code .` - Start development with AI
3. Ask Claude: "Show me the microservices architecture"
