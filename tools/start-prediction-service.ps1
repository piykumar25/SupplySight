# SupplySight - Start Prediction Engine Service
# This script starts the Prediction Engine Service with correct environment variables

# Set environment variables
$env:POSTGRES_HOST = "127.0.0.1"
$env:POSTGRES_PORT = "5433"
$env:POSTGRES_DB = "supplysight"
$env:POSTGRES_USER = "supplysight"
$env:POSTGRES_PASSWORD = "supplysight_secret_new"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"

Write-Host "Starting Prediction Engine Service on port 8085..." -ForegroundColor Green
Write-Host "Database: $env:POSTGRES_HOST:$env:POSTGRES_PORT/$env:POSTGRES_DB" -ForegroundColor Cyan

Set-Location -Path "$PSScriptRoot\..\services\prediction-engine-service"
mvn spring-boot:run
