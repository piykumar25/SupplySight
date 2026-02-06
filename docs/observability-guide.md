# Observability Guide

This guide explains how to view metrics, logs, and traces locally for the SupplySight platform.

## Quick Start

### 1. Start Infrastructure (with Observability)

```powershell
cd infra
docker-compose up -d
```

This starts:
- **Prometheus** (http://localhost:9090) - Metrics collection
- **Grafana** (http://localhost:3000) - Dashboards & visualization
- **Jaeger** (http://localhost:16686) - Distributed tracing

### 2. Start Backend Services

```powershell
cd tools
.\start-all.ps1 -Detached
```

### 3. Access Observability Tools

| Tool | URL | Credentials |
|------|-----|-------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Jaeger | http://localhost:16686 | - |

## Grafana Dashboards

Navigate to **Dashboards → SupplySight → SupplySight Overview** to see:

- **Service Health**: Up/down status of all services
- **Event Ingestion Throughput**: Events processed per second
- **Kafka Consumer Lag**: Message backlog per topic
- **API Latency P95/P99**: Response time percentiles
- **Prediction Engine Latency**: ML inference timing
- **Error Rate (5xx)**: Failure percentage by service
- **JVM Heap Usage**: Memory consumption
- **SSE Connections**: Active streaming connections

## Prometheus Metrics

### View Raw Metrics

Each service exposes metrics at `/actuator/prometheus`:

```powershell
# API Gateway
curl http://localhost:8080/actuator/prometheus

# Identity Service
curl http://localhost:8081/actuator/prometheus

# Event Ingestion
curl http://localhost:8082/actuator/prometheus
```

### Key Metrics

| Metric | Description |
|--------|-------------|
| `http_server_requests_seconds` | HTTP request latency histogram |
| `jvm_memory_used_bytes` | JVM memory usage |
| `kafka_consumer_fetch_manager_records_lag_max` | Kafka consumer lag |
| `hikaricp_connections_active` | Active DB connections |
| `http_server_requests_errors` | 5xx error count |

### Query Examples (in Prometheus UI)

```promql
# P95 latency by service
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, job))

# Error rate
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)

# Request throughput
sum(rate(http_server_requests_seconds_count[1m])) by (job)
```

## Distributed Tracing (Jaeger)

1. Open http://localhost:16686
2. Select a service from the dropdown
3. Click "Find Traces"
4. Click on a trace to see the full request flow

### Trace Context

Each trace includes:
- `correlationId`: Request correlation across services
- `tenantId`: Multi-tenant isolation context
- `userId`: Authenticated user
- Service-to-service call sequence
- Latency breakdown per span

## Structured Logging

### Development Mode (Console)
```
2026-02-06 17:45:00.123 [http-nio-8080-exec-1] INFO [abc-123] [tenant-001] [user-001] com.supplysight.gateway.filter - Request processed
```

### Production Mode (JSON)

Set `SPRING_PROFILES_ACTIVE=prod` to enable JSON logging:

```json
{
  "@timestamp": "2026-02-06T17:45:00.123+0530",
  "level": "INFO",
  "service": "api-gateway",
  "correlationId": "abc-123",
  "tenantId": "tenant-001",
  "userId": "user-001",
  "requestPath": "/api/v1/shipments",
  "requestMethod": "GET",
  "latencyMs": "45",
  "httpStatus": "200",
  "message": "Request processed"
}
```

## Alert Rules

Prometheus alert rules are defined in `infra/prometheus/alert-rules.yml`:

### System Alerts
| Alert | Condition | Severity |
|-------|-----------|----------|
| HighErrorRate | 5xx rate > 5% | Critical |
| HighAPILatency | P95 > 500ms | Warning |
| KafkaConsumerLag | Lag > 10,000 | Warning |
| ServiceDown | Service unreachable | Critical |
| HighJVMMemoryUsage | Heap > 85% | Warning |

### Tenant Quota Alerts
| Alert | Condition | Severity |
|-------|-----------|----------|
| TenantQuotaWarning | Usage > 80% | Warning |
| TenantQuotaCritical | Usage > 95% | Critical |
| HighThrottleRate | Throttled > 10/sec | Warning |
| SseConnectionsRejected | Rejected > 1/sec | Warning |

### SLO Error Budget Alerts
| Alert | Condition | Severity |
|-------|-----------|----------|
| SLOErrorBudgetFastBurn | Burn rate > 14.4x | Critical |
| SLOErrorBudgetSlowBurn | Burn rate > 2x | Warning |
| SLOErrorBudgetExhausted | Budget < 0% | Critical |
| SLOErrorBudgetLow | Budget < 25% | Warning |
| LatencySLOBreach | P95 > 300ms | Warning |

## Troubleshooting

### Prometheus Not Scraping Services

1. Check if services expose metrics:
   ```powershell
   curl http://localhost:8081/actuator/prometheus
   ```

2. Verify Prometheus targets:
   - Open http://localhost:9091/targets
   - Check for "DOWN" status

3. If running on Windows, ensure `host.docker.internal` resolves

### Grafana Dashboard Empty

1. Verify Prometheus datasource is configured
2. Check time range (top right corner)
3. Generate some traffic first:
   ```powershell
   curl http://localhost:8080/api/v1/login -X POST -H "Content-Type: application/json" -d '{"email":"admin@demo.com","password":"admin123"}'
   ```

### Jaeger Not Showing Traces

1. Check OTLP endpoint is reachable:
   ```powershell
   curl http://localhost:4318/v1/traces
   ```

2. Verify tracing is enabled in service config:
   ```yaml
   management.tracing.enabled: true
   ```
