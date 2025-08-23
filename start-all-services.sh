#!/bin/sh

echo "🚀 Starting Distributed Order Management System..."

# Create log directory
mkdir -p /var/log/supervisor

# Set environment variables for all services
export SPRING_PROFILES_ACTIVE=render
export DATABASE_URL=jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
export DATABASE_USERNAME=sa
export DATABASE_PASSWORD=
export DATABASE_DRIVER=org.h2.Driver
export HIBERNATE_DIALECT=org.hibernate.dialect.H2Dialect
export JWT_SECRET_KEY=renderSecretKeyForProduction2024
export JAVA_OPTS="-Xmx150m -XX:+UseContainerSupport"

# Wait for services to be ready
wait_for_service() {
    local port=$1
    local service_name=$2
    echo "⏳ Waiting for $service_name on port $port..."
    
    for i in $(seq 1 60); do
        if nc -z localhost $port; then
            echo "✅ $service_name is ready on port $port"
            return 0
        fi
        sleep 2
    done
    
    echo "❌ $service_name failed to start on port $port"
    return 1
}

# Start all services with supervisor
echo "🔄 Starting all services..."
exec supervisord -c /etc/supervisor/conf.d/supervisord.conf