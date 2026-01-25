# SupplySight - Start Visibility Projection Service
# This script starts the Visibility Projection Service with correct environment variables

# Set environment variables
$env:POSTGRES_HOST = "127.0.0.1"
$env:POSTGRES_PORT = "5433"
$env:POSTGRES_DB = "supplysight"
$env:POSTGRES_USER = "supplysight"
$env:POSTGRES_PASSWORD = "supplysight_secret_new"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"

Write-Host "Starting Visibility Projection Service on port 8084..." -ForegroundColor Green
Write-Host "Database: $env:POSTGRES_HOST:$env:POSTGRES_PORT/$env:POSTGRES_DB" -ForegroundColor Cyan

Set-Location -Path "$PSScriptRoot\..\services\visibility-projection-service"
mvn spring-boot:run
