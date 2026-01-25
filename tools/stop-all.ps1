# SupplySight - Stop All Services
# This script terminates all SupplySight background processes

Write-Host "Stopping SupplySight Services..." -ForegroundColor Yellow

# 1. Stop Backend Services (Java)
Write-Host "Stopping Backend Services (Java process)..." -ForegroundColor Cyan
try {
    Stop-Process -Name "java" -ErrorAction Stop
    Write-Host "   Backend Services Terminated." -ForegroundColor Green
} catch {
    Write-Host "   No running Java processes found." -ForegroundColor Gray
}

# 2. Stop Frontend (Node.js)
Write-Host "Stopping Frontend (Node.js process)..." -ForegroundColor Cyan
try {
    Stop-Process -Name "node" -ErrorAction Stop
    Write-Host "   Frontend Terminated." -ForegroundColor Green
} catch {
    Write-Host "   No running Node.js processes found." -ForegroundColor Gray
}

Write-Host ""
Write-Host "All background applications have been stopped." -ForegroundColor Green
Write-Host "Note: Docker containers are still running. To stop them, run: docker-compose down" -ForegroundColor Gray
