#!/usr/bin/env bash
set -euo pipefail

# Script simplificado - apenas Order Service como gateway principal
# Para troubleshooting do deploy no Render

echo "🚀 SIMPLIFIED START - Order Service only"

# Debug environment
echo "🔍 Environment:"
echo "PORT=${PORT:-'not set'}"
echo "PWD=$(pwd)"
echo "USER=$(whoami)"

# Check JARs
echo "📁 Available JARs:"
ls -la /app/*.jar

# Check port
PORT=${PORT:-8080}
echo "🔍 Using PORT=$PORT"

if nc -z localhost "$PORT" 2>/dev/null; then
    echo "⚠️ Port $PORT already in use!"
else
    echo "✅ Port $PORT available"
fi

# Set comprehensive environment
export SPRING_PROFILES_ACTIVE=render
export DATABASE_URL=jdbc:h2:mem:orderdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
export DATABASE_USERNAME=sa
export DATABASE_PASSWORD=
export DATABASE_DRIVER=org.h2.Driver
export HIBERNATE_DIALECT=org.hibernate.dialect.H2Dialect
export JAVA_OPTS="-Xmx400m -XX:+UseContainerSupport -Dserver.address=0.0.0.0"

echo "🔄 Starting Order Service directly on port $PORT..."
echo "📊 Java version:"
java -version

# Start service directly (no supervisord)
exec java $JAVA_OPTS \
    -Dspring.profiles.active=render \
    -Dserver.port="$PORT" \
    -Dserver.address=0.0.0.0 \
    -Dlogging.level.org.springframework.boot.web.embedded.tomcat=INFO \
    -Dlogging.level.org.apache.catalina=INFO \
    -jar /app/order-service.jar