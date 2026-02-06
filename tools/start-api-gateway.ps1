# Start API Gateway Service
# Port: 8080

$servicePath = Join-Path $PSScriptRoot "..\services\api-gateway"

if (-not (Test-Path $servicePath)) {
    Write-Host "Error: API Gateway service not found at $servicePath" -ForegroundColor Red
    exit 1
}

Write-Host "Starting API Gateway Service on port 8080..." -ForegroundColor Cyan
Set-Location $servicePath
mvn spring-boot:run
