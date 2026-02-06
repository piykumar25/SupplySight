# Scaling Recommendations

## Overview

This document provides guidance on scaling SupplySight services based on load characteristics.

## Current Architecture Limits

| Component | Default Capacity | Scaling Trigger |
|-----------|-----------------|-----------------|
| API Gateway | ~1000 req/s per instance | CPU > 70%, p95 > 200ms |
| Event Ingestion | ~500 events/s per instance | Kafka lag > 5000 |
| Visibility Projection | ~200 concurrent reads | DB pool > 60%, CPU > 70% |
| Prediction Engine | ~50 concurrent SSE | SSE connections > 80% |
| PostgreSQL | 100 connections | Connection wait > 100ms |
| Redis | 10,000 ops/s | Memory > 75% |
| Kafka | 10,000 msg/s | Partition lag > 10,000 |

---

## Horizontal Scaling Playbooks

### API Gateway
```bash
# Scale out when:
# - CPU > 70% sustained
# - Request queue depth > 100
# - p95 latency > 200ms

# Kubernetes scaling
kubectl scale deployment api-gateway --replicas=3

# Notes:
# - Stateless, scales linearly
# - Consider geo-distributed instances for latency
```

### Event Ingestion Service
```bash
# Scale out when:
# - Kafka consumer lag > 5000 messages
# - CPU > 80%
# - Event processing latency > 500ms

# Kubernetes scaling
kubectl scale deployment event-ingestion --replicas=3

# Notes:
# - Scale with Kafka partition count
# - Each replica handles separate partitions
# - Max replicas = number of partitions
```

### Visibility Projection Service
```bash
# Scale out when:
# - DB connection pool > 60%
# - Query latency p95 > 100ms
# - CPU > 70%

# Kubernetes scaling
kubectl scale deployment visibility-projection --replicas=3

# Notes:
# - Read-heavy, scales well horizontally
# - Consider read replicas for PostgreSQL
```

### Prediction Engine (SSE)
```bash
# Scale out when:
# - SSE connections > 80% of limit
# - Memory > 75%
# - Connection rejection rate > 0

# Kubernetes scaling
kubectl scale deployment prediction-engine --replicas=2

# Notes:
# - SSE connections are sticky
# - Use sticky sessions or connection draining
# - Each instance has connection limit
```

---

## Vertical Scaling Guidance

### PostgreSQL
| Metric | Action |
|--------|--------|
| CPU > 80% | Increase vCPUs |
| Connection wait > 100ms | Increase max_connections |
| Disk I/O wait > 20% | Move to SSD/NVMe |
| Memory < 2x working set | Increase RAM |

### Redis
| Metric | Action |
|--------|--------|
| Memory > 75% | Increase memory or expire keys |
| CPU > 60% | Consider Redis Cluster |
| Evictions > 0 | Increase memory |

### Kafka
| Metric | Action |
|--------|--------|
| Partition lag growing | Add partitions + consumers |
| Producer queue full | Scale brokers |
| Disk usage > 70% | Reduce retention or add disk |

---

## Capacity Planning

### Event Volume Growth
```
# Formula for event ingestion capacity
required_instances = (peak_events_per_second / 500) * 1.5

# Example: 2000 events/sec peak
# required_instances = (2000 / 500) * 1.5 = 6 instances
```

### Database Storage
```
# Estimated storage per tenant per month
events_storage = events_per_day * 30 * 1KB
shipments_storage = active_shipments * 5KB
alerts_storage = alerts_per_day * 30 * 0.5KB

# Plan for 50% headroom + 12 month growth
```

### Connection Limits
```
# Formula for SSE connections
max_sse_per_instance = 50
required_instances = max_concurrent_users / max_sse_per_instance

# Example: 200 concurrent users
# required_instances = 200 / 50 = 4 instances
```

---

## Cost Optimization

1. **Right-size instances**: Use metrics to identify over-provisioned services
2. **Auto-scaling**: Configure HPA based on CPU/memory/custom metrics
3. **Reserved capacity**: Use reserved instances for base load
4. **Spot/preemptible**: Use for batch processing (data purge jobs)
5. **Database optimization**: Use read replicas, connection pooling

---

## Monitoring Alerts for Scaling

| Alert | Threshold | Action |
|-------|-----------|--------|
| HighCPUUsage | > 70% for 5m | Scale horizontally |
| HighMemoryUsage | > 80% for 5m | Scale vertically or horizontally |
| HighKafkaLag | > 5000 for 5m | Add consumers |
| DBPoolExhausted | pending > 5 for 2m | Scale app or increase pool |
| SSEConnectionsNearLimit | > 80% for 5m | Scale prediction-engine |
