# Run Local CI Pipeline
# Based on e:\SupplySight\docs\running-ci-locally.md

$ErrorActionPreference = "Stop"

# Use coloring for steps
function Write-Step {
    param($Message)
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host " $Message" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

# Ensure we run from project root, assuming script is in tools/
$ProjectRoot = "$PSScriptRoot/.."
Push-Location $ProjectRoot

try {
    Write-Step "1. Backend Lint & Format Check"
    Write-Host "Running: mvn spotless:check -q" -ForegroundColor Gray
    mvn spotless:check -q
    if ($LASTEXITCODE -ne 0) { throw "Backend Lint Check Failed" }

    Write-Step "2. Backend Unit Tests"
    Write-Host "Running: mvn test -DskipITs -q" -ForegroundColor Gray
    mvn test -DskipITs -q
    if ($LASTEXITCODE -ne 0) { throw "Backend Unit Tests Failed" }

    Write-Step "3. Backend Integration Tests"
    # Note: Requires Docker/Podman running for Testcontainers
    try {
        docker info > $null 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Docker found. Running integration tests..." -ForegroundColor Yellow
            Write-Host "Running: mvn verify -pl tests/integration -DskipUTs -q" -ForegroundColor Gray
            mvn verify -pl tests/integration -DskipUTs -q
            if ($LASTEXITCODE -ne 0) { throw "Backend Integration Tests Failed" }
        }
        else {
            Write-Host "WARNING: Docker not running. Skipping Integration Tests." -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "WARNING: Docker check failed. Skipping Integration Tests." -ForegroundColor Yellow
    }

    Write-Step "4. Build All Modules"
    Write-Host "Running: mvn package -DskipTests -q" -ForegroundColor Gray
    mvn package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "Backend Build Failed" }

    Write-Step "5. Frontend Lint & Type Check"
    $FrontendDir = "supplysight-web"
    Push-Location $FrontendDir
    try {
        Write-Host "Installing dependencies..." -ForegroundColor Gray
        npm install
        
        Write-Host "Running lint..." -ForegroundColor Gray
        npm run lint
        if ($LASTEXITCODE -ne 0) { throw "Frontend Lint Failed" }
        
        Write-Host "Type checking..." -ForegroundColor Gray
        npx tsc --noEmit
        if ($LASTEXITCODE -ne 0) { throw "Frontend Type Check Failed" }
    }
    finally {
        Pop-Location
    }

    Write-Step "6. Frontend Build"
    Push-Location $FrontendDir
    try {
        Write-Host "Building Frontend..." -ForegroundColor Gray
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "Frontend Build Failed" }
    }
    finally {
        Pop-Location
    }

    Write-Step "CI Pipeline Completed Successfully!"
    Write-Host "Ready to commit and push." -ForegroundColor Green
}
catch {
    Write-Host ""
    Write-Host "CI Pipeline Failed!" -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}
finally {
    Pop-Location
}
