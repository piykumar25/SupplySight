# SupplySight - Start Identity Service
# This script starts the Identity Service with correct environment variables

# Set environment variables
# Use 127.0.0.1 instead of localhost to avoid IPv6 issues
$env:POSTGRES_HOST = "127.0.0.1"
$env:POSTGRES_PORT = "5433"
$env:POSTGRES_DB = "supplysight"
$env:POSTGRES_USER = "supplysight"
$env:POSTGRES_PASSWORD = "supplysight_secret_new"
$env:KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:JWT_SECRET = "supplysight-secret-key-that-should-be-at-least-256-bits-long-for-hs256"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Starting Identity Service (port 8081)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Database: $env:POSTGRES_HOST`:$env:POSTGRES_PORT/$env:POSTGRES_DB" -ForegroundColor Yellow
Write-Host "User: $env:POSTGRES_USER" -ForegroundColor Yellow
Write-Host ""

# Change to identity-service directory
Set-Location -Path "e:\SupplySight\services\identity-service"

# Run the service
mvn spring-boot:run
