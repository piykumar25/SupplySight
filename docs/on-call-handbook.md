# On-Call Handbook (TL;DR)

## Quick Reference

### Service URLs
| Service | Port | Health Check |
|---------|------|--------------|
| API Gateway | 8080 | /actuator/health |
| Identity | 8081 | /actuator/health |
| Event Ingestion | 8082 | /actuator/health |
| Tracking | 8083 | /actuator/health |
| Visibility | 8084 | /actuator/health |
| Prediction Engine | 8085 | /actuator/health |
| Audit | 8086 | /actuator/health |

### Dashboards
- **Grafana**: http://localhost:3000
- **Prometheus**: http://localhost:9090
- **Kibana**: http://localhost:5601

---

## Alert Response

### 🔴 CRITICAL

| Alert | First Response |
|-------|---------------|
| ServiceDown | Check pod logs, restart if needed |
| SLOErrorBudgetExhausted | Page engineering lead, feature freeze |
| HighErrorRate (>5%) | Check recent deployments, rollback |
| DatabaseConnectionPoolExhausted | Scale app or increase pool |

### 🟡 WARNING

| Alert | First Response |
|-------|---------------|
| SLOErrorBudgetLow | Monitor, reduce risky changes |
| SLOErrorBudgetSlowBurn | Review recent changes, focus on stability |
| HighAPILatency | Check DB queries, Redis, Kafka lag |
| KafkaConsumerLag | Scale consumers |
| TenantQuotaWarning | Monitor tenant usage, plan capacity |
| TenantQuotaCritical | Contact tenant, consider limit increase |
| LatencySLOBreach | Check Visibility API performance |

---

## Common Commands

### Check Service Status
```bash
kubectl get pods -l app=supplysight
kubectl logs -l app=visibility-service --tail=100
```

### Restart Service
```bash
kubectl rollout restart deployment/visibility-service
```

### Check Kafka Lag
```bash
kafka-consumer-groups.sh --bootstrap-server kafka:9092 \
  --describe --group supplysight-consumers
```

### Check PostgreSQL Connections
```sql
SELECT count(*) FROM pg_stat_activity WHERE state = 'active';
```

### Clear Redis Cache
```bash
redis-cli FLUSHDB
```

---

## Runbook Links

- [SSE Outage](runbooks/sse-outage.md)
- [Kafka DLQ Processing](runbooks/kafka-dlq.md)
- [Database Recovery](runbooks/db-recovery.md)
- [Service Restart](runbooks/service-restart.md)

---

## Escalation Matrix

| Severity | Response Time | Escalation |
|----------|--------------|------------|
| P1 (Down) | 15 min | Page on-call + manager |
| P2 (Degraded) | 30 min | Page on-call |
| P3 (Minor) | 2 hours | Slack channel |
| P4 (Cosmetic) | Next business day | Jira ticket |

---

## Contacts

| Role | Contact |
|------|---------|
| Primary On-Call | Check PagerDuty |
| Secondary On-Call | Check PagerDuty |
| Engineering Manager | @manager in Slack |
| Database DBA | @dba in Slack |
