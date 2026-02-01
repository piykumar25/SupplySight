# SupplySight - Data Seeding Orchestrator
# Checks service availability and seeds demo/alert data if needed

$Host.UI.RawUI.WindowTitle = "SupplySight - Data Seeding"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   SupplySight Automatic Data Seeding   " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Wait for Identity Service (Port 8081)
Write-Host "Waiting for Identity Service (8081) to be ready..." -ForegroundColor Yellow
$retries = 60 # 2 minutes
while ($retries -gt 0) {
    if (Test-NetConnection -ComputerName localhost -Port 8081 -InformationLevel Quiet) {
        Write-Host "Identity Service is UP." -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 2
    $retries--
    Write-Host "." -NoNewline -ForegroundColor Gray
}

if ($retries -eq 0) {
    Write-Host "`nTimeout waiting for Identity Service. Skipping seeding." -ForegroundColor Red
    Start-Sleep -Seconds 10
    exit
}

Write-Host ""

# 2. Seed Identity Data (Users/Tenants)
Write-Host "Checking Identity Data..." -ForegroundColor Yellow
try {
    # We call seed-demo-data.ps1. It will handle its own idempotency (usually) or error if data exists
    # To reduce noise, we can assume if login works, we skip?
    # But seed-demo-data doesn't fail destructively, so we just run it.
    cd "$PSScriptRoot"
    ./seed-demo-data.ps1
}
catch {
    Write-Host "Error running seed-demo-data.ps1: $_" -ForegroundColor Red
}

Write-Host ""

# 3. Seed Alerts Data
Write-Host "Checking Alerts Data..." -ForegroundColor Yellow
try {
    ./seed-alerts.ps1
}
catch {
    Write-Host "Error running seed-alerts.ps1: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   Data Verification Complete           " -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host "Closing in 5 seconds..." -ForegroundColor DarkGray
Start-Sleep -Seconds 5
