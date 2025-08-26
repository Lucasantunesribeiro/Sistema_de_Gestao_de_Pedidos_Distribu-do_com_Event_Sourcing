#!/bin/bash

# Sistema de Gestão de Pedidos - Database Migration Script
# Executa migrações de banco de dados para produção

set -e

echo "🗄️  Sistema de Gestão de Pedidos - Migração de Banco de Dados"
echo "=================================================================="

# Default values
DATABASE_URL=${DATABASE_URL:-""}
ENVIRONMENT=${ENVIRONMENT:-"development"}
DRY_RUN=${DRY_RUN:-false}

show_usage() {
    echo "Uso: $0 [opções]"
    echo ""
    echo "Opções:"
    echo "  -e, --env ENVIRONMENT    Ambiente (development, production)"
    echo "  -u, --url DATABASE_URL   URL do banco de dados"
    echo "  -d, --dry-run           Simular migrações sem executar"
    echo "  -h, --help              Mostrar esta ajuda"
    echo ""
    echo "Variáveis de ambiente:"
    echo "  DATABASE_URL            URL de conexão do PostgreSQL"
    echo "  DATABASE_USERNAME       Usuário do banco"
    echo "  DATABASE_PASSWORD       Senha do banco"
    echo ""
    echo "Exemplos:"
    echo "  $0 --env production --url postgresql://user:pass@host:5432/db"
    echo "  $0 --dry-run"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -u|--url)
            DATABASE_URL="$2"
            shift 2
            ;;
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            echo "❌ Opção desconhecida: $1"
            show_usage
            exit 1
            ;;
    esac
done

echo "🔧 Configuração:"
echo "   Environment: $ENVIRONMENT"
echo "   Dry Run: $DRY_RUN"

if [ -z "$DATABASE_URL" ]; then
    if [ "$ENVIRONMENT" = "production" ]; then
        echo "❌ DATABASE_URL é obrigatória para produção"
        exit 1
    fi
    
    # Default for development
    DATABASE_URL="jdbc:postgresql://localhost:5432/order_db"
    echo "   Database URL: $DATABASE_URL (padrão desenvolvimento)"
else
    echo "   Database URL: ${DATABASE_URL/postgresql:\/\//postgresql://***@}"
fi

echo ""

# Check database connectivity
echo "🔗 Testando conectividade com o banco..."

if [[ $DATABASE_URL == jdbc:postgresql://* ]]; then
    # Extract connection details from JDBC URL
    DB_HOST=$(echo $DATABASE_URL | sed -n 's/jdbc:postgresql:\/\/\([^:]*\):.*/\1/p')
    DB_PORT=$(echo $DATABASE_URL | sed -n 's/jdbc:postgresql:\/\/[^:]*:\([0-9]*\)\/.*/\1/p')
    DB_NAME=$(echo $DATABASE_URL | sed -n 's/jdbc:postgresql:\/\/[^\/]*\/\([^?]*\).*/\1/p')
else
    echo "❌ Formato de URL não suportado. Use formato JDBC."
    exit 1
fi

echo "   Host: $DB_HOST"
echo "   Port: $DB_PORT"
echo "   Database: $DB_NAME"

# Test connection
if command -v pg_isready &> /dev/null; then
    if ! pg_isready -h $DB_HOST -p $DB_PORT -d $DB_NAME; then
        echo "❌ Não foi possível conectar ao banco de dados"
        exit 1
    fi
    echo "   ✅ Conectividade confirmada"
else
    echo "   ⚠️  pg_isready não disponível, pulando teste de conectividade"
fi

# Run migrations for each service that uses database
run_service_migrations() {
    local service=$1
    local service_path="services/$service"
    
    echo ""
    echo "📋 Executando migrações para $service..."
    
    if [ ! -d "$service_path" ]; then
        echo "   ⚠️  Serviço $service não encontrado, pulando..."
        return
    fi
    
    cd "$service_path"
    
    # Check if service uses database
    if [ ! -f "pom.xml" ] || ! grep -q "flyway\|liquibase" pom.xml; then
        echo "   ℹ️  Serviço $service não usa migrações de banco, pulando..."
        cd ../..
        return
    fi
    
    if [ "$DRY_RUN" = true ]; then
        echo "   🔍 [DRY RUN] Simulando migrações para $service..."
        
        # Check for Flyway
        if grep -q "flyway" pom.xml; then
            echo "   📄 Flyway detectado - verificando scripts de migração..."
            if [ -d "src/main/resources/db/migration" ]; then
                ls -la src/main/resources/db/migration/ | grep -E "\.sql$" || echo "      Nenhum script encontrado"
            fi
        fi
        
        # Check for Liquibase
        if grep -q "liquibase" pom.xml; then
            echo "   📄 Liquibase detectado - verificando changelog..."
            if [ -f "src/main/resources/db/changelog/db.changelog-master.xml" ]; then
                echo "      Changelog encontrado"
            fi
        fi
    else
        echo "   🚀 Executando migrações para $service..."
        
        # Set environment for migration
        export DATABASE_URL="$DATABASE_URL"
        export SPRING_PROFILES_ACTIVE="$ENVIRONMENT"
        
        # Run Flyway migration
        if grep -q "flyway" pom.xml; then
            echo "   📄 Executando migração Flyway..."
            mvn flyway:migrate -Dflyway.url="$DATABASE_URL" -Dflyway.user="$DATABASE_USERNAME" -Dflyway.password="$DATABASE_PASSWORD"
        fi
        
        # Run Liquibase migration
        if grep -q "liquibase" pom.xml; then
            echo "   📄 Executando migração Liquibase..."
            mvn liquibase:update -Dliquibase.url="$DATABASE_URL" -Dliquibase.username="$DATABASE_USERNAME" -Dliquibase.password="$DATABASE_PASSWORD"
        fi
        
        # Run Spring Boot migration (if no specific tool)
        if ! grep -q "flyway\|liquibase" pom.xml; then
            echo "   🌱 Executando migração Spring Boot..."
            mvn spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.hibernate.ddl-auto=update --spring.profiles.active=$ENVIRONMENT" &
            SPRING_PID=$!
            sleep 10
            kill $SPRING_PID 2>/dev/null || true
        fi
    fi
    
    cd ../..
    echo "   ✅ Migrações para $service concluídas"
}

# Run migrations for services that use database
echo "🗄️  Executando migrações de banco de dados..."

run_service_migrations "order-service"
run_service_migrations "order-query-service"

# Create initial data if needed
if [ "$DRY_RUN" = false ] && [ "$ENVIRONMENT" != "production" ]; then
    echo ""
    echo "🌱 Criando dados iniciais para desenvolvimento..."
    
    # You can add SQL scripts here to populate initial data
    # psql "$DATABASE_URL" -c "INSERT INTO ..."
    
    echo "   ✅ Dados iniciais criados"
fi

echo ""
echo "🎉 Migrações concluídas com sucesso!"
echo "=================================================================="

if [ "$DRY_RUN" = true ]; then
    echo "ℹ️  Este foi um dry run. Para executar realmente:"
    echo "   $0 --env $ENVIRONMENT --url '$DATABASE_URL'"
fi

echo ""
echo "📊 Status final do banco:"
if command -v psql &> /dev/null && [ "$DRY_RUN" = false ]; then
    echo "   Tabelas criadas:"
    psql "$DATABASE_URL" -c "\dt" 2>/dev/null || echo "   Não foi possível listar tabelas"
else
    echo "   psql não disponível ou dry run ativo"
fi