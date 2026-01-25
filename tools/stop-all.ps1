# SupplySight - Stop All Services
# This script terminates all SupplySight background processes

Write-Host "Stopping SupplySight Services..." -ForegroundColor Yellow

# 1. Stop Backend Services (Java)
Write-Host "Stopping Backend Services (Java process)..." -ForegroundColor Cyan
try {
    Stop-Process -Name "java" -ErrorAction Stop
    Write-Host "   Backend Services Terminated." -ForegroundColor Green
}
catch {
    Write-Host "   No running Java processes found." -ForegroundColor Gray
}

# 2. Stop Frontend (Node.js)
Write-Host "Stopping Frontend (Node.js process)..." -ForegroundColor Cyan
try {
    Stop-Process -Name "node" -ErrorAction Stop
    Write-Host "   Frontend Terminated." -ForegroundColor Green
}
catch {
    Write-Host "   No running Node.js processes found." -ForegroundColor Gray
}

# 3. Stop Infrastructure (Docker)
Write-Host "Stopping Infrastructure (Docker)..." -ForegroundColor Cyan
if (Test-Path "../infra/docker-compose.yml") {
    Push-Location "../infra"
    try {
        docker-compose down
        Write-Host "   Infrastructure Stopped." -ForegroundColor Green
    }
    catch {
        Write-Host "   Failed to stop infrastructure: $_" -ForegroundColor Red
    }
    Pop-Location
}
else {
    Write-Host "   Warning: ../infra/docker-compose.yml not found." -ForegroundColor Red
}

Write-Host ""
Write-Host "All services and infrastructure have been stopped." -ForegroundColor Green
