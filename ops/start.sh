#!/usr/bin/env bash
set -euo pipefail

# Start backend in background
PORT=${PORT:-8080}
java -Dserver.port=$PORT -Dserver.address=0.0.0.0 ${JAVA_OPTS:-} -jar /app/app.jar &
BACK_PID=$!

# Wait for backend to be healthy (up to 60s)
ATTEMPTS=30
until curl -fsS http://127.0.0.1:$PORT/actuator/health >/dev/null || [ $ATTEMPTS -eq 0 ]; do
  echo "Aguardando backend... ($ATTEMPTS)"
  ATTEMPTS=$((ATTEMPTS-1))
  sleep 2
done

# Start Nginx in foreground
exec nginx -g 'daemon off;'

