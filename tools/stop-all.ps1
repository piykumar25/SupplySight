# SupplySight - Stop All Services
# This script terminates all SupplySight services and optionally stops infrastructure

param(
    [switch]$KeepInfra  # Keep Docker infrastructure running
)

Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "    Stopping SupplySight Services      " -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""

# Define SupplySight service ports
$servicePorts = @{
    "Identity Service"   = 8081
    "Tracking Service"   = 8083
    "Visibility Service" = 8084
    "Prediction Service" = 8085
    "Frontend"           = 5173
}

$stoppedCount = 0

# 1. Close PowerShell terminal windows spawned by start-all.ps1
Write-Host "1. Closing SupplySight Terminal Windows..." -ForegroundColor Cyan

$supplySightWindows = Get-Process powershell -ErrorAction SilentlyContinue | 
    Where-Object { $_.MainWindowTitle -like "SupplySight -*" }

if ($supplySightWindows) {
    foreach ($window in $supplySightWindows) {
        Write-Host "   Closing: $($window.MainWindowTitle)" -ForegroundColor Gray
        Stop-Process -Id $window.Id -Force -ErrorAction SilentlyContinue
    }
    Write-Host "   Closed $($supplySightWindows.Count) terminal window(s)." -ForegroundColor Green
} else {
    Write-Host "   No SupplySight terminal windows found." -ForegroundColor Gray
}

# 2. Stop Backend Services by Port (Java processes)
Write-Host ""
Write-Host "2. Stopping Backend Services..." -ForegroundColor Cyan

foreach ($service in @("Identity Service", "Tracking Service", "Visibility Service", "Prediction Service")) {
    $port = $servicePorts[$service]
    
    # Find process using the port
    $connection = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    
    if ($connection) {
        $processId = $connection.OwningProcess | Select-Object -First 1
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        
        if ($process) {
            Write-Host "   Stopping $service (PID: $processId, Port: $port)..." -ForegroundColor Gray
            Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
            Write-Host "   $service stopped." -ForegroundColor Green
            $stoppedCount++
        }
    } else {
        Write-Host "   $service not running (port $port)." -ForegroundColor DarkGray
    }
}

# 3. Stop Frontend (Node.js on port 5173)
Write-Host ""
Write-Host "3. Stopping Frontend..." -ForegroundColor Cyan

$frontendPort = $servicePorts["Frontend"]
$frontendConnection = Get-NetTCPConnection -LocalPort $frontendPort -State Listen -ErrorAction SilentlyContinue

if ($frontendConnection) {
    $processId = $frontendConnection.OwningProcess | Select-Object -First 1
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    
    if ($process) {
        Write-Host "   Stopping Frontend (PID: $processId, Port: $frontendPort)..." -ForegroundColor Gray
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
        Write-Host "   Frontend stopped." -ForegroundColor Green
        $stoppedCount++
    }
} else {
    Write-Host "   Frontend not running (port $frontendPort)." -ForegroundColor DarkGray
}

# 4. Stop Infrastructure (Docker) - unless -KeepInfra flag is set
Write-Host ""
if ($KeepInfra) {
    Write-Host "4. Keeping Infrastructure Running (-KeepInfra flag set)" -ForegroundColor Yellow
} else {
    Write-Host "4. Stopping Infrastructure (Docker)..." -ForegroundColor Cyan
    
    $infraPath = Join-Path $PSScriptRoot "..\infra\docker-compose.yml"
    if (Test-Path $infraPath) {
        Push-Location (Join-Path $PSScriptRoot "..\infra")
        try {
            docker-compose down 2>&1 | Out-Null
            Write-Host "   Infrastructure stopped." -ForegroundColor Green
        }
        catch {
            Write-Host "   Failed to stop infrastructure: $_" -ForegroundColor Red
        }
        Pop-Location
    }
    else {
        Write-Host "   Warning: docker-compose.yml not found at $infraPath" -ForegroundColor Red
    }
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
if ($stoppedCount -gt 0) {
    Write-Host "  Stopped $stoppedCount service(s) successfully!" -ForegroundColor Green
} else {
    Write-Host "  No running services were found." -ForegroundColor Yellow
}
if ($KeepInfra) {
    Write-Host "  Docker infrastructure is still running." -ForegroundColor Yellow
}
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
