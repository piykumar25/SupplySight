# Start Audit Service
# Port: 8086

$servicePath = Join-Path $PSScriptRoot "..\services\audit-service"

if (-not (Test-Path $servicePath)) {
    Write-Host "Error: Audit service not found at $servicePath" -ForegroundColor Red
    exit 1
}

Write-Host "Starting Audit Service on port 8086..." -ForegroundColor Cyan
Set-Location $servicePath
mvn spring-boot:run
