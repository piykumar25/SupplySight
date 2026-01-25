# SupplySight - Start All Services
# This script starts the entire stack in separate terminal windows

Write-Host "Starting SupplySight Stack..." -ForegroundColor Cyan

# 1. Start Infrastructure (Docker)
Write-Host "1. Checking Infrastructure..." -ForegroundColor Yellow
$dockerStatus = docker-compose -f ../infra/docker-compose.yml ps -q
if (-not $dockerStatus) {
    Write-Host "   Starting Docker containers..." -ForegroundColor Gray
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd ../infra; docker-compose up -d; Write-Host 'Infrastructure Started!'; Read-Host 'Press Enter to close...'"
    Write-Host "   Waiting 30s for databases to initialize..." -ForegroundColor Gray
    Start-Sleep -Seconds 30
} else {
    Write-Host "   Infrastructure is already running." -ForegroundColor Green
}

# 2. Start Backend Services
Write-Host "2. Starting Backend Services..." -ForegroundColor Yellow

# Identity Service
Write-Host "   Launching Identity Service..." -ForegroundColor Gray
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; .\start-identity-service.ps1"

# Wait a bit for Identity Service to initialize before others (optional but safer)
Start-Sleep -Seconds 5

# Tracking Service
Write-Host "   Launching Tracking Service..." -ForegroundColor Gray
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; .\start-tracking-service.ps1"

# Visibility Service
Write-Host "   Launching Visibility Service..." -ForegroundColor Gray
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; .\start-visibility-service.ps1"

# Prediction Service
Write-Host "   Launching Prediction Service..." -ForegroundColor Gray
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; .\start-prediction-service.ps1"

# 3. Start Frontend
Write-Host "3. Starting Frontend..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; .\start-frontend.ps1"

Write-Host ""
Write-Host "All services have been launched in separate windows!" -ForegroundColor Green
Write-Host "Frontend will be available at http://localhost:5173" -ForegroundColor Green
