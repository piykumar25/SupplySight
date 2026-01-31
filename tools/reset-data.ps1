# SupplySight - Reset Data and Restart
# STOPS services, WIPES all data volumes, and STARTS fresh.

Write-Host ""
Write-Host "WARNING: This will delete all database data!" -ForegroundColor Red
Write-Host "Stopping everything..." -ForegroundColor Yellow
.\stop-all.ps1

Write-Host ""
Write-Host "Wiping Docker Volumes..." -ForegroundColor Yellow
Push-Location ..\infra
docker-compose down -v
Pop-Location

Write-Host ""
Write-Host "Starting fresh..." -ForegroundColor Green
.\start-all.ps1
