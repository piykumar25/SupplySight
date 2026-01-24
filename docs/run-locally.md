# Running SupplySight Locally

This guide explains how to run the SupplySight Supply Chain Visibility Platform locally using Docker Compose.

## Prerequisites

- **Docker Desktop** (with Docker Compose v2)
- **Java 17** (for building services)
- **Maven 3.8+** (for building services)

## Quick Start

### 1. Start Infrastructure

Navigate to the infra directory and start all infrastructure services:

```bash
cd infra
docker-compose up -d
```

This starts:
- **PostgreSQL** on port `5432`
- **Redis** on port `6379`
- **Kafka** on port `9092`
- **Zookeeper** on port `2181`
- **Kafka UI** on port `8080` (optional, for debugging)
- **PgAdmin** on port `5050` (optional, for database management)

### 2. Verify Infrastructure

Check that all services are running:

```bash
docker-compose ps
```

All services should show as "healthy" or "running".

### 3. Build Services

From the project root:

```bash
# Build all services
mvn clean install -DskipTests

# Or build a specific service
mvn clean install -DskipTests -pl services/identity-service -am
```

### 4. Run Services

Each service can be run individually. Start with the Identity Service:

```bash
# Using Maven
cd services/identity-service
mvn spring-boot:run

# Or using the JAR
java -jar target/identity-service-1.0.0-SNAPSHOT.jar
```

Then start the Tracking Service (Port 8083):
```bash
cd services/tracking-service
mvn spring-boot:run
```

### 5. Start Frontend UI

The frontend is a React application located in `supplysight-web`.

```bash
cd supplysight-web
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

> **Note**: If backend services are not running, the frontend will automatically switch to **Demo Mode** (indicated by warnings) but remains fully functional for demonstration.


## Environment Variables

### Infrastructure Defaults

| Variable | Default Value | Description |
|----------|--------------|-------------|
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `supplysight` | Database name |
| `POSTGRES_USER` | `supplysight` | Database user |
| `POSTGRES_PASSWORD` | `supplysight_secret` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |

### JWT Configuration

| Variable | Default Value | Description |
|----------|--------------|-------------|
| `JWT_SECRET` | (dev default) | JWT signing secret (change in production!) |
| `JWT_ACCESS_TOKEN_VALIDITY_MS` | `3600000` | Access token validity (1 hour) |
| `JWT_REFRESH_TOKEN_VALIDITY_MS` | `86400000` | Refresh token validity (24 hours) |

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Identity Service | 8081 | Authentication & user management |
| Event Ingestion Service | 8082 | Event ingestion |
| Tracking Service | 8083 | Shipment management |
| Visibility Projection Service | 8084 | Real-time visibility |
| Prediction Engine Service | 8085 | ETA & predictions |
| Audit Service | 8086 | Audit logging |
| API Gateway | 8080 | Main entry point |

## Useful Commands

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f kafka
```

### Reset Infrastructure

```bash
# Stop and remove volumes
docker-compose down -v

# Restart fresh
docker-compose up -d
```

### Access Kafka UI

Open http://localhost:8080 in your browser to view Kafka topics and messages.

### Access PgAdmin

Open http://localhost:5050 in your browser.
- Email: `admin@supplysight.com`
- Password: `admin`

### Connect to PostgreSQL

```bash
docker exec -it supplysight-postgres psql -U supplysight -d supplysight
```

### View Kafka Topics

```bash
docker exec -it supplysight-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Seed Demo Data

After starting the Identity Service, run the seed script:

```bash
# Create demo tenant and users
curl -X POST http://localhost:8081/api/v1/seed/demo
```

This creates:
- Demo tenant: `demo-tenant`
- Admin user: `admin@demo.com` / `admin123`
- Ops user: `ops@demo.com` / `ops123`
- Viewer: `viewer@demo.com` / `viewer123`

## Testing Login

```bash
# Login as admin
curl -X POST http://localhost:8081/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@demo.com", "password": "admin123"}'
```

## Troubleshooting

### Kafka Not Starting

If Kafka fails to start, ensure Zookeeper is healthy:

```bash
docker-compose logs zookeeper
```

### Database Connection Issues

Verify PostgreSQL is accepting connections:

```bash
docker exec -it supplysight-postgres pg_isready
```

### Database Connection Issues (Windows/Docker)

If services fail with `FATAL: password authentication failed` despite correct credentials, it is likely a Docker networking issue on Windows.

**Workaround**:
1. Check container IP: `docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" supplysight-postgres`
2. Update `application.yml` to use `127.0.0.1` explicitly instead of `localhost`.
3. Ensure the password in `application.yml` matches exactly what is inside the container (verify with `docker exec`).

If ports are in use, modify `docker-compose.yml` or stop conflicting services.

### Out of Memory

Increase Docker's memory allocation to at least 4GB for running all services.
