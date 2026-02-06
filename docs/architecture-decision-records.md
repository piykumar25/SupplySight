# Architecture Decision Records (ADRs)

## ADR-001: Multi-Tenant Architecture

**Status**: Accepted  
**Date**: 2026-01-15

### Context
SupplySight needs to serve multiple customers (tenants) from a single deployment.

### Decision
Use a shared database with tenant ID column in all tables. Tenant context extracted from JWT and validated on every request.

### Consequences
- ✅ Simpler operations (single deployment)
- ✅ Cost-effective scaling
- ⚠️ Requires careful tenant isolation in queries
- ⚠️ Noisy neighbor potential (mitigated by quotas)

---

## ADR-002: Event-Driven Architecture with Kafka

**Status**: Accepted  
**Date**: 2026-01-15

### Context
Need to decouple event ingestion from processing for scalability and reliability.

### Decision
Use Apache Kafka as the event backbone. Services publish and consume events asynchronously.

### Consequences
- ✅ Decoupled services, independent scaling
- ✅ Event replay capability
- ✅ Natural partitioning for high throughput
- ⚠️ Eventual consistency (acceptable for this domain)
- ⚠️ Operational complexity of Kafka

---

## ADR-003: API Gateway Pattern

**Status**: Accepted  
**Date**: 2026-01-16

### Context
Need a single entry point for all client requests with cross-cutting concerns.

### Decision
Use Spring Cloud Gateway as the API gateway. Handle authentication, rate limiting, and routing centrally.

### Consequences
- ✅ Single entry point, simplified client
- ✅ Centralized rate limiting and auth
- ✅ Service discovery integration
- ⚠️ Single point of failure (mitigated by HA deployment)

---

## ADR-004: Redis for Real-Time Data

**Status**: Accepted  
**Date**: 2026-01-20

### Context
Need fast access to quota usage, rate limiting counters, and cached data.

### Decision
Use Redis for real-time counters (quotas, rate limits) and caching frequently accessed data.

### Consequences
- ✅ Sub-millisecond latency for quota checks
- ✅ Atomic counters for rate limiting
- ⚠️ Additional infrastructure component
- ⚠️ Need fallback strategy for Redis outage

---

## ADR-005: PostgreSQL with Schema-per-Service

**Status**: Accepted  
**Date**: 2026-01-16

### Context
Need persistent storage with ACID guarantees for core data.

### Decision
Use PostgreSQL with separate schemas per service (identity, visibility, prediction). Flyway for migrations.

### Consequences
- ✅ Strong consistency for critical data
- ✅ Schema isolation between services
- ✅ Mature tooling and ecosystem
- ⚠️ Schema changes require migrations

---

## ADR-006: JWT for Authentication

**Status**: Accepted  
**Date**: 2026-01-17

### Context
Need stateless authentication for microservices.

### Decision
Use JWT tokens with short expiry (15 min) and refresh tokens (7 days). Tenant ID and roles embedded in token claims.

### Consequences
- ✅ Stateless, scalable authentication
- ✅ Self-contained authorization info
- ⚠️ Cannot revoke tokens immediately (acceptable with short expiry)
- ⚠️ Token size overhead

---

## ADR-007: SSE for Real-Time Alerts

**Status**: Accepted  
**Date**: 2026-01-25

### Context
Users need real-time notifications for shipment alerts.

### Decision
Use Server-Sent Events (SSE) for push notifications. Simpler than WebSockets for one-way communication.

### Consequences
- ✅ Simple implementation
- ✅ Automatic reconnection in browsers
- ✅ Works through proxies/firewalls
- ⚠️ One-directional (sufficient for our use case)
- ⚠️ Connection limit per tenant needed

---

## ADR-008: Prometheus + Grafana for Observability

**Status**: Accepted  
**Date**: 2026-01-28

### Context
Need metrics, dashboards, and alerting for production monitoring.

### Decision
Use Prometheus for metrics collection with Micrometer instrumentation. Grafana for dashboards. Alerting rules in Prometheus.

### Consequences
- ✅ Industry standard, well-documented
- ✅ Rich query language (PromQL)
- ✅ Native Spring Boot support
- ⚠️ Requires metric retention strategy
