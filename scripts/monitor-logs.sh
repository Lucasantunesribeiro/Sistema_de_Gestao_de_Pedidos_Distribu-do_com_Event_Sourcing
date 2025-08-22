#!/bin/bash
# Monitor de logs agregados

echo "📊 Monitorando logs dos microsserviços..."
echo "Pressione Ctrl+C para parar"
echo ""

docker-compose logs -f --tail=50 order-db query-db rabbitmq redis &

if pgrep -f "order-service" > /dev/null; then
    echo "📱 Incluindo logs dos serviços Java..."
    tail -f services/*/logs/*.log 2>/dev/null &
fi

wait
