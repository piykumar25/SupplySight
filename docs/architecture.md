# SupplySight Architecture

## Overview

SupplySight is a production-grade, multi-tenant SaaS platform for Supply Chain Visibility, Real-time Tracking, and Predictive Analytics. The platform uses an event-driven architecture with Kafka for reliable message processing and provides real-time visibility into shipment status across the supply chain.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              External Systems                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                     │
│  │GPS Devices│  │ Scanners │  │ Partner  │  │ IoT      │                     │
│  │          │  │          │  │ Webhooks │  │ Sensors  │                     │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘                     │
└───────┼─────────────┼─────────────┼─────────────┼───────────────────────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                              │
                    ┌─────────▼─────────┐
                    │   API Gateway     │
                    │  (Rate Limiting,  │
                    │   JWT Validation) │
                    └─────────┬─────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼───────┐   ┌─────────▼─────────┐   ┌──────▼──────┐
│   Identity    │   │  Event Ingestion  │   │  Tracking   │
│   Service     │   │     Service       │   │   Service   │
│ (Auth, Users) │   │ (Validation,      │   │ (Shipment   │
│               │   │  Deduplication)   │   │    CRUD)    │
└───────────────┘   └─────────┬─────────┘   └─────────────┘
                              │
                    ┌─────────▼─────────┐
                    │      Kafka        │
                    │  Message Broker   │
                    └─────────┬─────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼───────┐   ┌─────────▼─────────┐   ┌──────▼──────┐
│  Visibility   │   │   Prediction      │   │   Audit     │
│  Projection   │   │     Engine        │   │   Service   │
│   Service     │   │    Service        │   │             │
└───────┬───────┘   └─────────┬─────────┘   └─────────────┘
        │                     │
        │                     │
┌───────▼───────┐   ┌─────────▼─────────┐
│    Redis      │   │   PostgreSQL      │
│  (Cache)      │   │   (Persistent)    │
└───────────────┘   └───────────────────┘
```

## Services

### 1. API Gateway
- **Port**: 8080
- **Responsibilities**:
  - JWT validation and authentication
  - Rate limiting per tenant
  - Request routing to downstream services
  - Correlation ID injection
  - Central access logging

### 2. Identity Service
- **Port**: 8081
- **Responsibilities**:
  - Tenant management (CRUD)
  - User management (registration, updates)
  - Authentication (login, JWT issuance)
  - Role-based access control (ADMIN, OPS_USER, VIEWER)
  - Password hashing (BCrypt)

### 3. Event Ingestion Service
- **Port**: 8082
- **Responsibilities**:
  - REST API for event ingestion
  - Kafka consumer for raw events
  - Schema validation
  - Event deduplication by eventId
  - Publishing validated events to Kafka

### 4. Tracking Service
- **Port**: 8083
- **Responsibilities**:
  - Shipment CRUD operations
  - Shipment lifecycle management
  - Publishing shipment lifecycle events

### 5. Visibility Projection Service
- **Port**: 8084
- **Responsibilities**:
  - Consuming validated events
  - Updating materialized views (current state)
  - Building shipment timelines
  - Redis caching for low-latency reads
  - Real-time visibility APIs (<300ms response)

### 6. Prediction Engine Service
- **Port**: 8085
- **Responsibilities**:
  - ETA computation
  - Delay probability analysis
  - Anomaly detection
  - Alert generation for high-risk shipments
  - ML-ready feature extraction

### 7. Audit Service
- **Port**: 8086
- **Responsibilities**:
  - Consuming audit events from Kafka
  - Append-only audit log persistence
  - Compliance reporting
  - Audit trail queries

## Data Flow

### Event Ingestion Flow

```
1. External System → API Gateway → Event Ingestion Service
2. Event Ingestion Service validates and dedupes
3. Valid events → Kafka (tracking.events.validated)
4. Invalid events → Rejected with error response
```

### Projection Flow

```
1. Kafka (tracking.events.validated) → Visibility Projection Service
2. Update shipment_current_state (Postgres + Redis)
3. Append to shipment_timeline (Postgres)
4. Handle out-of-order events by eventTime ordering
```

### Prediction Flow

```
1. Kafka (tracking.events.validated) → Prediction Engine Service
2. Compute ETA, delay probability, anomaly flags
3. Store prediction in shipment_prediction
4. If alert threshold crossed → Kafka (tracking.alerts)
```

## Kafka Topics

| Topic | Description | Partitions |
|-------|-------------|------------|
| `tracking.events.raw` | Raw events from external sources | 3 |
| `tracking.events.validated` | Validated and enriched events | 3 |
| `tracking.predictions` | Prediction results | 3 |
| `tracking.alerts` | High-priority alerts | 3 |
| `tracking.audit` | Audit events | 3 |

## Database Schema

### Identity Schema
- `tenants` - Tenant information
- `users` - User accounts (tenant-scoped)

### Tracking Schema
- `tracking_events` - Immutable event store
- `processed_events` - Deduplication tracking

### Visibility Schema
- `shipment_current_state` - Materialized current state
- `shipment_timeline` - Immutable timeline

### Prediction Schema
- `shipment_predictions` - Prediction results

### Audit Schema
- `audit_logs` - Append-only audit trail

## Multi-Tenancy

### Isolation Strategy
1. **JWT Claims**: `tenantId` embedded in JWT
2. **Request Context**: TenantContext ThreadLocal
3. **Database**: All queries filtered by `tenant_id`
4. **API**: Tenant validation on every request
5. **Kafka**: Events include `tenantId` for routing

### Tenant-Aware Queries
```sql
-- Every query includes tenant_id filter
SELECT * FROM shipments 
WHERE tenant_id = :tenantId AND id = :shipmentId;
```

## Security

### Authentication
- OAuth2/JWT-based authentication
- BCrypt password hashing
- Refresh token rotation
- Token blacklisting on logout

### Authorization
- Role-based access control (RBAC)
- Three roles: ADMIN, OPS_USER, VIEWER
- Endpoint-level permission checks

### Audit
- All significant actions logged
- Immutable audit trail
- Compliance-ready reporting

## Observability

### Metrics
- Prometheus endpoint: `/actuator/prometheus`
- Service-specific metrics
- JVM and GC metrics
- Kafka consumer/producer metrics

### Health Checks
- Liveness: `/actuator/health/liveness`
- Readiness: `/actuator/health/readiness`

### Logging
- Structured JSON logging
- Correlation ID propagation
- Tenant ID in log context

## Performance Targets

| Metric | Target |
|--------|--------|
| API Response Time (p99) | <300ms |
| Event Ingestion Throughput | 10,000 events/sec |
| Query Latency (cached) | <50ms |
| Query Latency (uncached) | <200ms |

## Scalability

### Horizontal Scaling
- All services are stateless
- Kafka partitioning for parallel processing
- Redis cluster for cache scaling
- Database read replicas for read scaling

### Vertical Scaling
- JVM tuning for high throughput
- Connection pooling (HikariCP)
- Batch processing where applicable
