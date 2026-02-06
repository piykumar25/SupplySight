# Start Event Ingestion Service
# Port: 8082

$servicePath = Join-Path $PSScriptRoot "..\services\event-ingestion-service"

if (-not (Test-Path $servicePath)) {
    Write-Host "Error: Event Ingestion service not found at $servicePath" -ForegroundColor Red
    exit 1
}

Write-Host "Starting Event Ingestion Service on port 8082..." -ForegroundColor Cyan
Set-Location $servicePath
mvn spring-boot:run
