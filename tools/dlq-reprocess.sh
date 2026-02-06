#!/bin/bash
# DLQ Reprocessing Script
# Republishes messages from a DLQ topic to the original topic for reprocessing.
#
# Usage:
#   ./dlq-reprocess.sh <dlq-topic> [--from <timestamp>] [--to <timestamp>] [--dry-run]
#
# Examples:
#   ./dlq-reprocess.sh tracking.events.raw.dlq
#   ./dlq-reprocess.sh tracking.events.raw.dlq --from "2026-02-01T00:00:00Z" --to "2026-02-01T23:59:59Z"
#   ./dlq-reprocess.sh tracking.events.raw.dlq --dry-run

set -e

KAFKA_BOOTSTRAP=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}
DLQ_TOPIC=$1
DRY_RUN=false
FROM_TIMESTAMP=""
TO_TIMESTAMP=""

# Parse arguments
shift
while [[ $# -gt 0 ]]; do
    case $1 in
        --from)
            FROM_TIMESTAMP=$2
            shift 2
            ;;
        --to)
            TO_TIMESTAMP=$2
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

if [ -z "$DLQ_TOPIC" ]; then
    echo "Usage: $0 <dlq-topic> [--from <timestamp>] [--to <timestamp>] [--dry-run]"
    exit 1
fi

# Derive original topic from DLQ topic
ORIGINAL_TOPIC=${DLQ_TOPIC%.dlq}

echo "=========================================="
echo "DLQ Reprocessing Script"
echo "=========================================="
echo "DLQ Topic:       $DLQ_TOPIC"
echo "Original Topic:  $ORIGINAL_TOPIC"
echo "Kafka Bootstrap: $KAFKA_BOOTSTRAP"
echo "Dry Run:         $DRY_RUN"
[ -n "$FROM_TIMESTAMP" ] && echo "From:            $FROM_TIMESTAMP"
[ -n "$TO_TIMESTAMP" ] && echo "To:              $TO_TIMESTAMP"
echo "=========================================="

# Check if DLQ topic exists
if ! kafka-topics.sh --bootstrap-server "$KAFKA_BOOTSTRAP" --list | grep -q "^$DLQ_TOPIC$"; then
    echo "ERROR: DLQ topic '$DLQ_TOPIC' does not exist"
    exit 1
fi

# Get message count
MESSAGE_COUNT=$(kafka-run-class.sh kafka.tools.GetOffsetShell \
    --broker-list "$KAFKA_BOOTSTRAP" \
    --topic "$DLQ_TOPIC" \
    --time -1 | awk -F: '{sum += $3} END {print sum}')

echo "Messages in DLQ: $MESSAGE_COUNT"

if [ "$MESSAGE_COUNT" -eq 0 ]; then
    echo "No messages to reprocess."
    exit 0
fi

read -p "Proceed with reprocessing? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 0
fi

# Reprocess messages
PROCESSED=0
FAILED=0

if [ "$DRY_RUN" = true ]; then
    echo "[DRY RUN] Would reprocess $MESSAGE_COUNT messages"
    kafka-console-consumer.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP" \
        --topic "$DLQ_TOPIC" \
        --from-beginning \
        --max-messages 10 \
        --property print.headers=true
else
    echo "Starting reprocessing..."
    
    # Use kafka-mirror-maker or simple consumer/producer
    kafka-console-consumer.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP" \
        --topic "$DLQ_TOPIC" \
        --from-beginning \
        --property print.key=true \
        --property key.separator="|" \
    | while IFS='|' read -r key value; do
        if [ -n "$value" ]; then
            echo "$value" | kafka-console-producer.sh \
                --bootstrap-server "$KAFKA_BOOTSTRAP" \
                --topic "$ORIGINAL_TOPIC" \
                --property parse.key=true \
                --property key.separator="|"
            ((PROCESSED++))
        fi
    done
    
    echo "=========================================="
    echo "Reprocessing complete!"
    echo "Processed: $PROCESSED"
    echo "Failed:    $FAILED"
    echo "=========================================="
fi
