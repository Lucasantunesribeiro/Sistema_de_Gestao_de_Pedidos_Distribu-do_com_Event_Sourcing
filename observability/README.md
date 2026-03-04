# Observability Stack

Complete observability solution for the Unified Order System.

## Components

### Prometheus (Port 9090)
- Metrics collection from application
- Alert evaluation
- Time-series database
- Scrapes metrics every 15 seconds

### Grafana (Port 3000)
- Visualization platform
- Pre-configured dashboards
- Multi-datasource support (Prometheus, Loki, Tempo)
- Default credentials: admin/admin

### Loki (Port 3100)
- Log aggregation
- JSON log parsing
- 30-day retention
- Indexed by labels

### Tempo (Port 3200)
- Distributed tracing
- Zipkin/OTLP compatible
- 7-day retention
- Linked to logs via correlation IDs

### Alertmanager (Port 9093)
- Alert routing and grouping
- Multiple receiver support (Slack, Email, PagerDuty)
- Alert inhibition rules

### Promtail
- Log shipping to Loki
- Docker log collection
- JSON parsing

### Node Exporter (Port 9100)
- Host metrics (CPU, Memory, Disk)
- System-level monitoring

### Postgres Exporter (Port 9187)
- Database metrics
- Connection pool monitoring
- Query performance

## Directory Structure

```
observability/
├── prometheus/
│   ├── prometheus.yml      # Prometheus configuration
│   └── alerts.yml          # Alert rules
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/    # Auto-configured datasources
│   │   └── dashboards/     # Dashboard provisioning
│   └── dashboards/         # Dashboard JSON files
│       ├── system-overview.json
│       └── business-metrics.json
├── loki/
│   └── loki-config.yaml    # Loki configuration
├── promtail/
│   └── promtail-config.yaml # Promtail configuration
├── tempo/
│   └── tempo-config.yaml   # Tempo configuration
└── alertmanager/
    └── alertmanager.yml    # Alert routing configuration
```

## Quick Start

### Start Everything

```bash
# From project root
./scripts/start-observability.sh
```

### Manual Start

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Start application
docker-compose -f docker-compose.services.yml up -d unified-order-system

# 3. Start observability
docker-compose -f docker-compose.observability.yml up -d
```

### Verify

```bash
# Check all services
docker-compose -f docker-compose.observability.yml ps

# Test endpoints
curl http://localhost:9090/-/healthy  # Prometheus
curl http://localhost:3000/api/health # Grafana
curl http://localhost:3100/ready      # Loki
curl http://localhost:3200/ready      # Tempo
curl http://localhost:9093/-/healthy  # Alertmanager
```

## Accessing the UI

| Service | URL | Credentials |
|---------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | None |
| Alertmanager | http://localhost:9093 | None |

## Dashboards

### System Overview
- Service status
- Request rate and latency
- Error rate
- JVM metrics
- Database connection pool

Access: Grafana → Dashboards → System Overview

### Business Metrics
- Order creation metrics
- Payment processing
- Inventory reservations
- Success/failure rates

Access: Grafana → Dashboards → Business Metrics

## Alerting

### Configured Alerts

| Alert | Severity | Threshold | Action |
|-------|----------|-----------|--------|
| ServiceDown | Critical | Service down >1min | Immediate |
| HighErrorRate | Warning | Error rate >5% | Investigate |
| CriticalErrorRate | Critical | Error rate >10% | Immediate |
| HighLatencyP95 | Warning | P95 >1s | Investigate |
| HighMemoryUsage | Warning | Heap >85% | Monitor |
| CriticalMemoryUsage | Critical | Heap >95% | Immediate |
| DatabaseDown | Critical | DB down >1min | Immediate |

### Testing Alerts

```bash
# Trigger high error rate
for i in {1..100}; do
  curl http://localhost:8090/api/orders/nonexistent
done

# Check alerts in Alertmanager
curl http://localhost:9093/api/v2/alerts
```

## Customization

### Adding New Metrics

1. Add to `MetricsConfig.java`:
```java
@Bean
public Counter myCustomCounter(MeterRegistry registry) {
    return Counter.builder("my.custom.metric")
        .description("Description")
        .register(registry);
}
```

2. Use in code:
```java
@Autowired
private Counter myCustomCounter;

public void myMethod() {
    myCustomCounter.increment();
}
```

3. Query in Prometheus:
```promql
rate(my_custom_metric_total[5m])
```

### Adding New Dashboard

1. Create dashboard in Grafana UI
2. Export JSON: Dashboard Settings → JSON Model
3. Save to `observability/grafana/dashboards/my-dashboard.json`
4. Restart Grafana: `docker-compose -f docker-compose.observability.yml restart grafana`

### Configuring Alerts

1. Edit `observability/prometheus/alerts.yml`
2. Add new rule:
```yaml
- alert: MyCustomAlert
  expr: my_metric > 100
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "My custom alert"
    description: "Detailed description"
```
3. Reload Prometheus: `curl -X POST http://localhost:9090/-/reload`

## Troubleshooting

### Prometheus Not Scraping

```bash
# Check targets
curl http://localhost:9090/api/v1/targets

# Check service discovery
docker logs prometheus
```

### Grafana Not Showing Data

```bash
# Test datasource
curl http://localhost:3000/api/datasources/proxy/1/api/v1/query?query=up

# Check Grafana logs
docker logs grafana
```

### Loki Not Receiving Logs

```bash
# Check Promtail
docker logs promtail

# Test Loki query
curl -G http://localhost:3100/loki/api/v1/query_range \
  --data-urlencode 'query={service="unified-order-system"}' \
  --data-urlencode 'limit=10'
```

## Performance Tuning

### Prometheus

```yaml
# prometheus.yml
global:
  scrape_interval: 15s     # Increase for less load
  evaluation_interval: 15s

storage:
  tsdb:
    retention.time: 15d    # Adjust based on needs
```

### Loki

```yaml
# loki-config.yaml
limits_config:
  retention_period: 720h   # 30 days
  ingestion_rate_mb: 10
  max_query_series: 500
```

### Tempo

```yaml
# tempo-config.yaml
compactor:
  compaction:
    block_retention: 720h  # 30 days
```

## Backup and Restore

### Backup Prometheus Data

```bash
docker run --rm -v prometheus_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/prometheus-backup-$(date +%Y%m%d).tar.gz /data
```

### Backup Grafana Dashboards

```bash
# Export all dashboards
curl -H "Authorization: Bearer <API_KEY>" \
  http://localhost:3000/api/search | jq -r '.[].uid' | \
  xargs -I {} curl -H "Authorization: Bearer <API_KEY>" \
  http://localhost:3000/api/dashboards/uid/{} > dashboards-backup.json
```

## Production Considerations

### Security

1. Change default Grafana password
2. Configure authentication (LDAP, OAuth)
3. Enable HTTPS
4. Restrict network access

### Scaling

1. Use remote storage for Prometheus (Thanos, Cortex)
2. Deploy Loki in microservices mode
3. Configure Tempo with object storage
4. Set up Grafana HA

### Monitoring the Monitors

- Monitor Prometheus with itself
- Set up dead man's switch alert
- Configure external health checks
- Set up backup alerting channel

## Documentation

- [Observability Guide](../OBSERVABILITY_GUIDE.md)
- [Runbook](../RUNBOOK.md)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

## Support

For issues or questions:
1. Check logs: `docker logs <service>`
2. Review documentation
3. Check GitHub issues
4. Contact SRE team
