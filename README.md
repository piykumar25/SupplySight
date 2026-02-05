# SupplySight - Supply Chain Visibility Platform

A production-grade, multi-tenant SaaS platform for Supply Chain Visibility, Real-time Tracking, and Predictive Analytics.

## Features

- **Real-time Event Ingestion**: Ingest shipment/container/vehicle events from GPS devices, scanners, and partner webhooks
- **Event-driven Architecture**: Kafka-based pipeline with at-least-once semantics and idempotency
- **Low-latency Visibility**: Materialized views with <300ms typical response time
- **Prediction Engine**: ETA prediction, delay risk analysis, and anomaly detection
- **Multi-tenant Isolation**: Enforced at API, database, and service layers
- **Enterprise Security**: OAuth2/JWT, RBAC, audit logging, input validation, rate limiting
- **Full Observability**: Structured logs, Prometheus metrics, health endpoints

## Technology Stack

- **Backend**: Java 17, Spring Boot 3, Spring Security, Spring Data JPA
- **Messaging**: Apache Kafka with spring-kafka
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis for current-state caching
- **Testing**: JUnit 5, Testcontainers for integration tests
- **Documentation**: OpenAPI/Swagger

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway                               │
│              (JWT validation, rate limiting, routing)            │
└─────────────────────────────────────────────────────────────────┘
                                │
    ┌───────────────────────────┼───────────────────────────┐
    │                           │                           │
┌───▼───┐                 ┌─────▼─────┐              ┌──────▼──────┐
│Identity│                 │  Event    │              │  Tracking   │
│Service │                 │ Ingestion │              │   Service   │
└────────┘                 └─────┬─────┘              └─────────────┘
                                 │
                          ┌──────▼──────┐
                          │    Kafka    │
                          └──────┬──────┘
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │                            │                            │
┌───▼────────┐           ┌───────▼───────┐           ┌───────▼───────┐
│ Visibility │           │  Prediction   │           │    Audit      │
│ Projection │           │    Engine     │           │   Service     │
└────────────┘           └───────────────┘           └───────────────┘
```

## Quick Start

### Prerequisites

- Docker Desktop with Docker Compose v2
- Java 17 (for building services)
- Maven 3.8+

### 1. Start Infrastructure

```bash
cd infra
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Kafka (port 9092)
- Zookeeper (port 2181)
- Kafka UI (port 9090)
- PgAdmin (port 5050)

Wait for all services to be healthy (check with `docker-compose ps`).

### 2. Build All Services

```bash
# From project root
mvn clean install -DskipTests
```

### 3. Start Services

**Option A: One-Click Start (Recommended - Background)**
Run the entire stack in the background without opening multiple terminals.

```powershell
cd tools
.\start-all.ps1 -Detached
```

- **Logs**: Output is saved to `tools/logs/`.
- **Status**: Run `.\status.ps1` to check services.
- **Stop**: Run `.\stop-all.ps1` to stop everything.

**Option B: Interactive Mode**
Open separate terminal windows for each service to see logs in real-time.

```powershell
cd tools
.\start-all.ps1
```

**Option C: Manual Start (Development)**
See [docs/run-locally.md](docs/run-locally.md) for detailed manual execution steps.


### 4. Seed Demo Data

```bash
curl -X POST http://localhost:8081/api/v1/seed/demo
```

### 5. Login via API Gateway

```bash
curl -X POST http://localhost:8080/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@demo.com", "password": "admin123"}'
```

Save the `accessToken` from the response for subsequent API calls.

### 6. Generate Sample Data

```bash
# Start sample data generator
cd tools/sample-data-generator
mvn spring-boot:run

# In another terminal, generate 10 shipments with events
curl -X POST http://localhost:8090/api/v1/generate/shipments/10 \
  -H "Content-Type: application/json"
```

### 7. Test End-to-End Flow

```bash
# 1. Ingest an event
curl -X POST http://localhost:8080/api/v1/events/ingest \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "550e8400-e29b-41d4-a716-446655440001",
    "tenantId": "<tenant-id-from-login>",
    "shipmentId": "550e8400-e29b-41d4-a716-446655440002",
    "eventType": "IN_TRANSIT",
    "eventTime": "2026-01-23T10:20:30Z",
    "source": "GPS_DEVICE",
    "location": {"lat": 12.9716, "lon": 77.5946, "hubCode": "BLR-HUB-01"},
    "payload": {"speedKmph": 62}
  }'

# 2. Query shipment visibility
curl http://localhost:8080/api/v1/shipments/<shipment-id> \
  -H "Authorization: Bearer <your-token>"

# 3. Get predictions
curl http://localhost:8080/api/v1/predictions/<shipment-id> \
  -H "Authorization: Bearer <your-token>"

# 4. View audit logs
curl http://localhost:8080/api/v1/audit/logs \
  -H "Authorization: Bearer <your-token>"
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Central entry point with JWT validation |
| Identity Service | 8081 | Authentication & user management |
| Event Ingestion Service | 8082 | Event validation & deduplication |
| Tracking Service | 8083 | Shipment CRUD operations |
| Visibility Projection Service | 8084 | Real-time visibility APIs |
| Prediction Engine Service | 8085 | ETA & delay predictions |
| Audit Service | 8086 | Audit logging |

## Kafka Topics

| Topic | Description |
|-------|-------------|
| `tracking.events.raw` | Raw events from external sources |
| `tracking.events.validated` | Validated and enriched events |
| `tracking.predictions` | Prediction results |
| `tracking.alerts` | High-priority alerts |
| `tracking.audit` | Audit events |

## API Documentation

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI spec: http://localhost:8081/api-docs
- Full API spec: [docs/api-spec.md](docs/api-spec.md)

## Demo Users

After seeding demo data:

| Email | Password | Role |
|-------|----------|------|
| admin@demo.com | admin123 | ADMIN |
| ops@demo.com | ops123 | OPS_USER |
| viewer@demo.com | viewer123 | VIEWER |

## Project Structure

```
supply-chain-platform/
├── services/
│   ├── common/                    # Shared DTOs, utilities, security
│   ├── api-gateway/               # Spring Cloud Gateway
│   ├── identity-service/          # Auth & user management
│   ├── event-ingestion-service/   # Event validation & publishing
│   ├── tracking-service/          # Shipment CRUD
│   ├── visibility-projection-service/  # Real-time views
│   ├── prediction-engine-service/ # ML predictions
│   └── audit-service/             # Audit logging
├── infra/
│   ├── docker-compose.yml
│   ├── kafka/
│   ├── postgres/
│   └── redis/
├── docs/
│   ├── architecture.md
│   ├── api-spec.md
│   └── run-locally.md
└── tools/
    ├── sample-data-generator/
    └── load-test/
```

## Running Tests

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run tests for a specific service
mvn test -pl services/identity-service
```

### Integration Tests

Integration tests use Testcontainers and require Docker to be running.

```bash
# Run all integration tests (requires Docker)
mvn verify

# Run integration tests for a specific service
mvn verify -pl services/event-ingestion-service

# Run end-to-end integration tests
cd tests/integration
mvn verify
```

### Test Coverage

```bash
# Generate test coverage report
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Observability

### Metrics

All services expose Prometheus metrics at `/actuator/prometheus`:

```bash
# View metrics for a service
curl http://localhost:8080/actuator/prometheus

# Key metrics:
# - api.request.duration - API latency
# - api.request.total - Request count
# - kafka.messages.consumed - Kafka consumption rate
# - shipments.active - Active shipment count
```

### Health Checks

```bash
# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness

# Full health check
curl http://localhost:8080/actuator/health
```

### Logging

All services use structured logging with correlation IDs:
- Development: Console logging with correlation ID in MDC
- Production: JSON logging (Logstash format)

Correlation IDs are automatically propagated through:
- API Gateway → Downstream services
- Kafka message headers
- HTTP request headers (`X-Correlation-ID`)

## Monitoring & Debugging

### Kafka UI

Access Kafka UI at http://localhost:9090 to:
- View topics and messages
- Monitor consumer groups
- Inspect message payloads

### PgAdmin

Access PgAdmin at http://localhost:5050:
- Email: admin@supplysight.com
- Password: admin

Connect to PostgreSQL:
- Host: postgres (or localhost if connecting from host)
- Port: 5432
- Database: supplysight
- Username: supplysight
- Password: supplysight_secret

### Service Ports

| Service | Port | Health Check |
|---------|------|--------------|
| API Gateway | 8080 | http://localhost:8080/actuator/health |
| Identity Service | 8081 | http://localhost:8081/actuator/health |
| Event Ingestion | 8082 | http://localhost:8082/actuator/health |
| Visibility Projection | 8084 | http://localhost:8084/actuator/health |
| Prediction Engine | 8085 | http://localhost:8085/actuator/health |
| Audit Service | 8086 | http://localhost:8086/actuator/health |
| Sample Data Generator | 8090 | N/A |

## Troubleshooting

### Services Won't Start

1. **Check infrastructure is running:**
   ```bash
   cd infra
   docker-compose ps
   ```

2. **Check service logs:**
   ```bash
   docker-compose logs -f <service-name>
   ```

3. **Verify ports are not in use:**
   ```bash
   # Windows
   netstat -ano | findstr :8080
   
   # Linux/Mac
   lsof -i :8080
   ```

### Kafka Connection Issues

1. **Verify Kafka is healthy:**
   ```bash
   docker exec supplysight-kafka kafka-topics --bootstrap-server localhost:9092 --list
   ```

2. **Check Kafka UI:** http://localhost:9090

### Database Connection Issues

1. **Verify PostgreSQL is running:**
   ```bash
   docker exec supplysight-postgres pg_isready -U supplysight
   ```

2. **Check database schema:**
   ```bash
   docker exec -it supplysight-postgres psql -U supplysight -d supplysight -c "\dn"
   ```

### JWT Token Issues

- Tokens expire after 1 hour (configurable)
- Use refresh token endpoint: `POST /api/v1/refresh`
- Verify JWT secret matches across all services

## Performance Testing

### Load Testing

Use the sample data generator for load testing:

```bash
# Generate 1000 shipments with events
curl -X POST http://localhost:8090/api/v1/generate/shipments/1000
```

### Benchmarking

Key performance targets:
- API Response Time (p99): <300ms
- Event Ingestion Throughput: 10,000 events/sec
- Query Latency (cached): <50ms
- Query Latency (uncached): <200ms

## Documentation

### Technical References
- **[System Design Document](DESIGN.md)** - Comprehensive architecture documentation including:
  - 👵 "Grandmother" non-technical summary
  - 🏗️ C4 Architecture Diagrams (System Context, Container, Sequence)
  - 📊 Database ERD (Entity Relationship Diagram)
  - 📝 Architectural Decision Records (ADRs)
  - 🔧 Operational Excellence & Observability

### Additional Docs
- [Architecture Overview](docs/architecture.md)
- [API Specification](docs/api-spec.md)
- [Local Development Guide](docs/run-locally.md)

## Production Deployment

### Environment Variables

Key environment variables to configure:

```bash
# JWT Secret (MUST be changed in production)
JWT_SECRET=your-secure-random-secret-key-min-32-chars

# Database
DB_HOST=postgres-host
DB_PORT=5432
DB_NAME=supplysight
DB_USERNAME=supplysight
DB_PASSWORD=secure-password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Redis
REDIS_HOST=redis-host
REDIS_PORT=6379
```

### Docker Deployment

Each service includes a `Dockerfile`. Build and deploy:

```bash
# Build service image
cd services/identity-service
docker build -t supplysight/identity-service:1.0.0 .

# Run container
docker run -p 8081:8081 \
  -e DB_HOST=postgres \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  supplysight/identity-service:1.0.0
```

## Contributing

1. Follow the existing code style
2. Write tests for new features
3. Update documentation as needed
4. Create meaningful commit messages

## License

Apache 2.0
