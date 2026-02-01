# SupplySight - Seed Alerts Data
# This script injects dummy alert data directly into the database

Write-Host "Checking alert data..." -ForegroundColor Cyan

# Check if postgres container is running
if (!(docker ps -q -f name=supplysight-postgres)) {
    Write-Host "Error: supplysight-postgres container is not running." -ForegroundColor Red
    exit 1
}

try {
    # Check if alerts already exist
    $count = docker exec -i supplysight-postgres psql -U supplysight -d supplysight -t -c "SELECT count(*) FROM prediction.alerts"
    $count = [int]$count.Trim()

    if ($count -gt 0) {
        Write-Host "Alerts already exist ($count found). Skipping seed." -ForegroundColor Yellow
        exit 0
    }

    Write-Host "Seeding new alerts..." -ForegroundColor Cyan
    
    # Pipe SQL content into docker exec psql
    # -i required for interactive mode to accept stdin
    Get-Content $PSScriptRoot\seed_alerts_data.sql | docker exec -i supplysight-postgres psql -U supplysight -d supplysight
    
    Write-Host "Alerts seeded successfully!" -ForegroundColor Green
}
catch {
    Write-Host "Failed to seed alerts: $_" -ForegroundColor Red
}
