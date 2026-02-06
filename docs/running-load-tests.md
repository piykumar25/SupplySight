# Running k6 Load Tests

This guide covers running load tests for the SupplySight platform.

## Prerequisites

1. **Install k6**:
   ```powershell
   # Windows (using chocolatey)
   choco install k6
   
   # Or download from https://k6.io/docs/get-started/installation/
   ```

2. **Start SupplySight Services**:
   ```powershell
   cd infra && docker-compose up -d
   cd ../tools && .\start-all.ps1 -Detached
   ```

3. **Get JWT Token**:
   ```powershell
   $response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/login" `
     -Method POST `
     -ContentType "application/json" `
     -Body '{"email":"admin@demo.com","password":"admin123"}'
   
   $TOKEN = $response.data.accessToken
   echo $TOKEN
   ```

## Available Test Scripts

| Script | Purpose | Target |
|--------|---------|--------|
| `event-ingestion.js` | Event ingestion throughput | Event Ingestion Service |
| `sse-churn.js` | SSE connection resilience | Visibility Service |
| `frontend-api.js` | User journey simulation | API Gateway |

## Running Tests

### Event Ingestion Load Test
```powershell
cd tests/load

# Smoke test only
k6 run --env BASE_URL=http://localhost:8082 --env TOKEN=$TOKEN `
  --env SCENARIO=smoke event-ingestion.js

# Full test (smoke + load + stress)
k6 run --env BASE_URL=http://localhost:8082 --env TOKEN=$TOKEN `
  event-ingestion.js
```

### SSE Churn Test
```powershell
k6 run --env BASE_URL=http://localhost:8084 --env TOKEN=$TOKEN `
  sse-churn.js
```

### Frontend API Test
```powershell
k6 run --env BASE_URL=http://localhost:8080 frontend-api.js
```

## Understanding Results

### Key Metrics
| Metric | Description | Target |
|--------|-------------|--------|
| `http_req_duration` | Request latency | P95 < 500ms |
| `success_rate` | Successful requests | > 95% |
| `events_accepted` | Events processed | > 0 |
| `ingestion_latency` | Event ingestion time | P95 < 200ms |

### Output Example
```
checks.........................: 98.50% ✓ 985  ✗ 15
http_req_duration..............: avg=125ms min=45ms med=98ms max=890ms p(90)=245ms p(95)=380ms
events_accepted................: 950    95.0%
events_rejected................: 35     3.5%
success_rate...................: 98.50%
```

## Custom Scenarios

### Increase Load Duration
```powershell
k6 run --duration 10m event-ingestion.js
```

### More Virtual Users
```powershell
k6 run --vus 100 --duration 5m event-ingestion.js
```

### Output to JSON
```powershell
k6 run --out json=results.json event-ingestion.js
```

## Grafana Dashboard

View real-time metrics during load tests:
1. Open http://localhost:3000
2. Navigate to SupplySight → SupplySight Overview
3. Watch API latency, throughput, and error rates

## Baseline Results (Reference)

Recorded on dev machine (i7, 16GB RAM):

| Test | VUs | RPS | P95 Latency | Error Rate |
|------|-----|-----|-------------|------------|
| Event Ingestion | 50 | 250 | 180ms | 0.5% |
| SSE Churn | 100 | N/A | 2.1s | 2.1% |
| Frontend API | 30 | 45 | 420ms | 0.8% |

## Troubleshooting

### High Error Rate
- Check service logs: `Get-Content logs/*.log -Tail 50`
- Verify database connections
- Check for rate limiting

### Timeout Errors
- Increase timeout in script
- Check service responsiveness
- Review connection pool settings

### Low Throughput
- Check CPU/memory usage
- Review JVM heap settings
- Consider horizontal scaling
