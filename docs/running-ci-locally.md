# Running CI Locally

This guide explains how to run the CI pipeline steps locally before pushing changes.

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 20+
- Docker (for Testcontainers)

## Quick Start

```powershell
# Run all checks
.\tools\run-ci-local.ps1

# Or run individual steps below
```

## Step-by-Step

### 1. Backend Lint & Format Check
```powershell
mvn spotless:check -q
# Auto-fix formatting:
mvn spotless:apply
```

### 2. Backend Unit Tests
```powershell
mvn test -DskipITs -q
# View results: target/surefire-reports/
```

### 3. Backend Integration Tests
```powershell
# Ensure Docker is running (for Testcontainers)
mvn verify -pl tests/integration -DskipUTs -q
# View results: tests/integration/target/failsafe-reports/
```

### 4. Build All Modules
```powershell
mvn package -DskipTests -q
```

### 5. Frontend Lint & Type Check
```powershell
cd supplysight-web
npm ci
npm run lint
npx tsc --noEmit
```

### 6. Frontend Build
```powershell
cd supplysight-web
npm run build
```

### 7. E2E Tests (Playwright)
```powershell
# Start backend services first
cd tools
.\start-all.ps1 -Detached

# Run E2E tests
cd ../tests/e2e
npm ci
npx playwright install chromium
npx playwright test

# View report
npx playwright show-report
```

## CI Environment Variables

When running locally, these are the default values:

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_HOST` | `localhost` | Database host |
| `POSTGRES_PORT` | `5432` | Database port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `JWT_SECRET` | (hardcoded) | JWT signing key |

## Troubleshooting

### Testcontainers Issues
```powershell
# Ensure Docker daemon is running
docker ps

# If using Podman, set:
$env:TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
$env:DOCKER_HOST="unix:///var/run/docker.sock"
```

### Port Conflicts
```powershell
# Check if ports are in use
netstat -ano | findstr "8080 8081 8082"

# Kill conflicting processes
Stop-Process -Id <PID> -Force
```

### Maven Cache Issues
```powershell
# Clear and rebuild
mvn clean
Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository\com\supplysight
mvn install -DskipTests
```

## GitHub Actions Locally

Use [act](https://github.com/nektos/act) to run GitHub Actions locally:

```powershell
# Install act
winget install nektos.act

# Run CI workflow
act -W .github/workflows/ci-cd.yml

# Run specific job
act -j backend-unit-tests
```
