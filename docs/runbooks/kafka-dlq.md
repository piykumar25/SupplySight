# Kafka DLQ Processing Runbook

## Overview
This runbook covers handling messages in Dead Letter Queue (DLQ) topics for the SupplySight platform.

## DLQ Topics
| Original Topic | DLQ Topic |
|----------------|-----------|
| `tracking.events.raw` | `tracking.events.raw.dlq` |
| `tracking.events.validated` | `tracking.events.validated.dlq` |
| `tracking.alerts` | `tracking.alerts.dlq` |
| `tracking.audit` | `tracking.audit.dlq` |

## Quick Check

### 1. View DLQ Message Count
```bash
# Using Kafka UI (http://localhost:8010)
# Navigate to Topics → [topic].dlq → Messages

# Or via CLI
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic tracking.events.raw.dlq \
  --from-beginning \
  --max-messages 10
```

### 2. Check DLQ Lag
```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group dlq-reprocessor
```

## DLQ Message Headers

Each DLQ message contains:
| Header | Description |
|--------|-------------|
| `x-original-topic` | Original topic name |
| `x-original-partition` | Original partition |
| `x-original-offset` | Original offset |
| `x-original-timestamp` | Original message timestamp |
| `x-error-message` | Error that caused failure |
| `x-error-stacktrace` | Stack trace (truncated) |
| `x-retry-count` | Number of retry attempts |
| `x-dlq-timestamp` | When message was sent to DLQ |
| `x-correlation-id` | Correlation ID for tracing |

## Reprocessing DLQ Messages

### Option 1: Manual Reprocessing Script
```bash
# Reprocess all messages from a DLQ topic
./tools/dlq-reprocess.sh tracking.events.raw.dlq

# Reprocess specific time range
./tools/dlq-reprocess.sh tracking.events.raw.dlq \
  --from "2026-02-01T00:00:00Z" \
  --to "2026-02-01T23:59:59Z"
```

### Option 2: Selective Reprocessing
```bash
# View messages first
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic tracking.events.raw.dlq \
  --from-beginning \
  --max-messages 100 \
  > dlq-messages.json

# Filter and republish
cat dlq-messages.json | jq 'select(.eventType == "CREATED")' | \
  kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic tracking.events.raw
```

### Option 3: API-Based Reprocessing
```bash
curl -X POST http://localhost:8082/api/internal/dlq/reprocess \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "tracking.events.raw.dlq",
    "maxMessages": 100,
    "filter": {
      "errorType": "ValidationException"
    }
  }'
```

## Common DLQ Scenarios

### Scenario 1: Schema Validation Errors
**Cause**: Invalid event payload
**Fix**: 
1. Identify malformed events
2. Fix source system
3. Discard or manually fix DLQ messages

### Scenario 2: Database Constraint Violations
**Cause**: Duplicate keys, FK violations
**Fix**:
1. Check if duplicate
2. If duplicate, discard
3. If FK issue, ensure parent record exists

### Scenario 3: Transient Failures
**Cause**: Temporary DB/network issues
**Fix**:
1. Wait for services to recover
2. Reprocess entire DLQ batch

### Scenario 4: Bug in Consumer
**Cause**: NPE, logic error
**Fix**:
1. Deploy fix
2. Reprocess DLQ messages
3. Monitor for new failures

## Cleanup

### After Successful Reprocessing
```bash
# Clear DLQ topic (DESTRUCTIVE)
kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --delete \
  --topic tracking.events.raw.dlq

# Recreate (optional - will auto-create on next failure)
kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic tracking.events.raw.dlq \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000  # 7 days
```

## Monitoring

### Grafana Dashboard
Navigate to: SupplySight → Kafka DLQ

### Key Metrics
- `kafka_dlq_messages_total` - Count of DLQ messages
- `kafka_dlq_lag` - Unprocessed DLQ messages
- `kafka_dlq_reprocessed_total` - Successfully reprocessed

### Alert Rules
```yaml
- alert: DLQMessagesHigh
  expr: kafka_dlq_messages_total > 100
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: DLQ has {{ $value }} unprocessed messages
```

## Escalation

1. **< 100 messages**: On-call investigates
2. **100-1000 messages**: Escalate to backend team
3. **> 1000 messages**: Incident, involve platform team
