# SSE Outage Runbook

## Overview
This runbook covers troubleshooting and resolution steps for Server-Sent Events (SSE) outages in the SupplySight platform.

## Symptoms
- Frontend shows "Connection Lost" or stale data
- Grafana alert: `HighSSEDisconnectRate`
- Users report not seeing real-time updates
- High SSE disconnect rate in metrics

## Quick Diagnosis (< 2 min)

### 1. Check SSE Service Health
```bash
curl http://localhost:8084/actuator/health
```
Expected: `{"status":"UP"}`

### 2. Check Active Connections
```bash
curl http://localhost:9091/api/v1/query?query=sse_connections_active
```

### 3. Check Disconnect Rate
```bash
curl http://localhost:9091/api/v1/query?query=rate(sse_disconnects_total[5m])
```

## Common Issues

### Issue 1: High Memory Usage
**Symptoms**: OOM errors, slow responses, connection drops

**Check**:
```bash
curl http://localhost:8084/actuator/metrics/jvm.memory.used
```

**Resolution**:
1. Restart the visibility-projection-service
2. Consider scaling horizontally
3. Review for memory leaks in SSE emitters

### Issue 2: Kafka Consumer Lag
**Symptoms**: Stale data, events not reflected

**Check**:
```bash
curl http://localhost:9091/api/v1/query?query=kafka_consumer_fetch_manager_records_lag_max
```

**Resolution**:
1. Check Kafka broker health
2. Increase consumer concurrency:
   ```yaml
   spring.kafka.listener.concurrency: 5
   ```
3. Restart affected consumers

### Issue 3: Connection Limits
**Symptoms**: New connections rejected, `Connection refused`

**Check**:
```bash
# Check open file descriptors
lsof -p $(pgrep -f visibility-projection) | wc -l
```

**Resolution**:
1. Increase ulimit: `ulimit -n 65536`
2. Add connection pooling
3. Implement connection limits per tenant

### Issue 4: Network Issues
**Symptoms**: Intermittent disconnects, timeouts

**Check**:
```bash
# Check network connectivity
ping localhost
netstat -an | grep 8084
```

**Resolution**:
1. Check API Gateway proxy timeout settings
2. Verify load balancer health checks
3. Review firewall rules

## Recovery Steps

### Restart Single Service
```powershell
# Stop
Stop-Process -Name "java" -Force | Where-Object {$_.CommandLine -like "*visibility*"}

# Start
cd e:\SupplySight\services\visibility-projection-service
mvn spring-boot:run
```

### Restart All Services (Nuclear Option)
```powershell
cd e:\SupplySight\tools
.\stop-all.ps1
Start-Sleep -Seconds 5
.\start-all.ps1 -Detached
```

### Force Client Reconnection
Clients should auto-reconnect, but if needed:
1. Clear browser cache
2. Refresh page
3. Log out and log back in

## Escalation Path

1. **Level 1 (On-call)**: Check health, restart services
2. **Level 2 (Backend Team)**: Debug Kafka, memory issues
3. **Level 3 (Platform Team)**: Infrastructure, networking

## Post-Incident

- [ ] Update this runbook with new learnings
- [ ] Create incident report
- [ ] Add monitoring for root cause
- [ ] Schedule retrospective
