# Running SupplySight Locally

This guide explains how to run the SupplySight Supply Chain Visibility Platform locally using Docker Compose.

> **See also:** [System Design Document](../DESIGN.md) for architecture diagrams and [API Specification](api-spec.md) for endpoint details.

## Prerequisites

- **Docker Desktop** (with Docker Compose v2)
- **Java 17** (for building services)
- **Maven 3.8+** (for building services)

## Quick Start (Recommended)

We have created automated scripts to make running the platform easy.

### 1. Start Everything

Open PowerShell in the `tools` directory and run:

```powershell
cd tools
.\start-all.ps1 -Detached
```

This script will:
1.  Start Docker Infrastructure (Postgres, Kafka, Redis) if not running.
2.  Build and Start all Backend Services (API Gateway, Identity, Event Ingestion, Tracking, Visibility, Prediction, Audit).
3.  Start the Frontend Application.
4.  **Automatically Validates & Seeds** demo data (Admin user, shipments, alerts).

### 2. Access the Application

- **Frontend**: [http://localhost:5173](http://localhost:5173)
- **Login**: `admin@demo.com` / `admin123`

### 3. Check Service Status

```powershell
.\status.ps1
```

Shows which services are running and their ports.

### 4. Stop Everything

```powershell
.\stop-all.ps1
```

**Options:**
- `.\stop-all.ps1` - Stop all services AND Docker infrastructure
- `.\stop-all.ps1 -KeepInfra` - Stop services but keep Docker running (faster restart)

---

## Manual Startup (Alternative)

If you prefer to run services manually to see console output:

### 1. Start Infrastructure

```bash
cd infra
docker-compose up -d
```

### 2. Run Services
Run each service in a separate terminal:

```bash
# API Gateway (Port 8080) - Start first
cd services/api-gateway
mvn spring-boot:run

# Identity Service (Port 8081)
cd services/identity-service
mvn spring-boot:run

# Event Ingestion Service (Port 8082)
cd services/event-ingestion-service
mvn spring-boot:run

# Tracking Service (Port 8083)
cd services/tracking-service
mvn spring-boot:run

# Visibility Service (Port 8084)
cd services/visibility-projection-service
mvn spring-boot:run

# Prediction Service (Port 8085)
cd services/prediction-engine-service
mvn spring-boot:run

# Audit Service (Port 8086)
cd services/audit-service
mvn spring-boot:run
```

### 3. Run Frontend

```bash
cd supplysight-web
npm run dev
```

---

## Data Seeding

**Automatic:**
Data is seeded automatically on startup.
- **Identity Service**: Creates `demo-tenant` and users (`admin`, `ops`, `viewer`).
- **Visibility Service**: Creates 5 dummy shipments if none exist.
- **Prediction Service**: Seeds sample alerts.

**Manual Reset:**
If you need to reset the data, you can use the SQL CLI or delete the Docker volumes.

---

## Troubleshooting

### "Invalid Credentials" after Restart
If the database volume persists old data incompatible with code changes:
1.  Stop everything: `.\stop-all.ps1`
2.  Reset Database Password (if needed):
    ```bash
    docker exec supplysight-postgres psql -U supplysight -d supplysight -c "ALTER USER supplysight WITH PASSWORD 'supplysight_secret_new';"
    ```
3.  Or Wipe Data (Fresh Start):
    ```bash
    docker-compose down -v
    ```

### "403 Forbidden" on Dashboard
If you see 403 errors despite being logged in:
1.  **Clear Browser Storage**: DevTools -> Application -> Local Storage -> Delete All.
2.  **Relogin**: Get a fresh token signed with the correct key.

### Services Not Starting
Check logs in `tools/logs/` directory.
```powershell
Get-Content tools/logs/identity-service.log -Tail 100
```
