
param(
    [switch]$Detached
)

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($Detached) {
    Write-Host " Starting SupplySight Stack (Background)" -ForegroundColor Cyan
}
else {
    Write-Host "    Starting SupplySight Stack         " -ForegroundColor Cyan
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Define service ports for duplicate detection
$servicePorts = @{
    "Identity"   = 8081
    "Tracking"   = 8083
    "Visibility" = 8084
    "Prediction" = 8085
    "Frontend"   = 5173
}

# Function to check if port is in use
function Test-PortInUse {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $connection
}

# 1. Start Infrastructure (Docker)
Write-Host "1. Checking Infrastructure..." -ForegroundColor Yellow
$dockerStatus = docker-compose -f ../infra/docker-compose.yml ps -q 2>$null
if (-not $dockerStatus) {
    Write-Host "   Starting Docker containers..." -ForegroundColor Gray
    
    if ($Detached) {
        Start-Process powershell -ArgumentList "-Command", "cd ../infra; docker-compose up -d" -Wait
    }
    else {
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd ../infra; docker-compose up -d; Write-Host 'Infrastructure Started!' -ForegroundColor Green; Read-Host 'Press Enter to close...'"
    }
    
    Write-Host "   Waiting 30s for databases to initialize..." -ForegroundColor Gray
    Start-Sleep -Seconds 30
}
else {
    Write-Host "   Infrastructure is already running." -ForegroundColor Green
}

# 2. Start Services
Write-Host ""
if ($Detached) {
    Write-Host "2. Starting Backend Services (Hidden Windows)..." -ForegroundColor Yellow

    # Create logs directory
    $logsDir = Join-Path $PSScriptRoot "logs"
    New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
    
    # PID tracking file for stop script
    $pidFile = Join-Path $logsDir "service-pids.txt"
    "# SupplySight Service PIDs - $(Get-Date)" | Out-File $pidFile

    # Identity Service
    if (Test-PortInUse -Port $servicePorts["Identity"]) {
        Write-Host "   Identity Service already running on port $($servicePorts["Identity"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        $proc = Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-identity-service.ps1" -PassThru -RedirectStandardOutput "$logsDir\identity-service.log" -RedirectStandardError "$logsDir\identity-service-error.log" -WindowStyle Hidden
        "Identity:$($proc.Id)" | Out-File $pidFile -Append
        Write-Host "   Started Identity Service (PID: $($proc.Id))" -ForegroundColor Green
    }

    # Wait a bit
    Start-Sleep -Seconds 5

    # Tracking Service
    if (Test-PortInUse -Port $servicePorts["Tracking"]) {
        Write-Host "   Tracking Service already running on port $($servicePorts["Tracking"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        $proc = Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-tracking-service.ps1" -PassThru -RedirectStandardOutput "$logsDir\tracking-service.log" -RedirectStandardError "$logsDir\tracking-service-error.log" -WindowStyle Hidden
        "Tracking:$($proc.Id)" | Out-File $pidFile -Append
        Write-Host "   Started Tracking Service (PID: $($proc.Id))" -ForegroundColor Green
    }

    # Visibility Service
    if (Test-PortInUse -Port $servicePorts["Visibility"]) {
        Write-Host "   Visibility Service already running on port $($servicePorts["Visibility"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        $proc = Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-visibility-service.ps1" -PassThru -RedirectStandardOutput "$logsDir\visibility-service.log" -RedirectStandardError "$logsDir\visibility-service-error.log" -WindowStyle Hidden
        "Visibility:$($proc.Id)" | Out-File $pidFile -Append
        Write-Host "   Started Visibility Service (PID: $($proc.Id))" -ForegroundColor Green
    }

    # Prediction Service
    if (Test-PortInUse -Port $servicePorts["Prediction"]) {
        Write-Host "   Prediction Service already running on port $($servicePorts["Prediction"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        $proc = Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-prediction-service.ps1" -PassThru -RedirectStandardOutput "$logsDir\prediction-service.log" -RedirectStandardError "$logsDir\prediction-service-error.log" -WindowStyle Hidden
        "Prediction:$($proc.Id)" | Out-File $pidFile -Append
        Write-Host "   Started Prediction Service (PID: $($proc.Id))" -ForegroundColor Green
    }

    # 3. Start Frontend
    Write-Host ""
    Write-Host "3. Starting Frontend (Hidden Window)..." -ForegroundColor Yellow
    if (Test-PortInUse -Port $servicePorts["Frontend"]) {
        Write-Host "   Frontend already running on port $($servicePorts["Frontend"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        $proc = Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-frontend.ps1" -PassThru -RedirectStandardOutput "$logsDir\frontend.log" -RedirectStandardError "$logsDir\frontend-error.log" -WindowStyle Hidden
        "Frontend:$($proc.Id)" | Out-File $pidFile -Append
        Write-Host "   Started Frontend (PID: $($proc.Id))" -ForegroundColor Green
    }

    # Summary
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host " All services are now running!         " -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Logs: $logsDir" -ForegroundColor Cyan
    Write-Host "PIDs: $pidFile" -ForegroundColor Cyan

}
else {
    # INTERACTIVE MODE (Original Behavior)
    Write-Host "2. Starting Backend Services..." -ForegroundColor Yellow
    
    # Identity Service
    if (Test-PortInUse -Port $servicePorts["Identity"]) {
        Write-Host "   Identity Service already running on port $($servicePorts["Identity"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        Write-Host "   Launching Identity Service..." -ForegroundColor Gray
        $title = "SupplySight - Identity Service (8081)"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = '$title'; cd '$PSScriptRoot'; .\start-identity-service.ps1"
    }

    # Wait a bit
    Start-Sleep -Seconds 5

    # Tracking Service
    if (Test-PortInUse -Port $servicePorts["Tracking"]) {
        Write-Host "   Tracking Service already running on port $($servicePorts["Tracking"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        Write-Host "   Launching Tracking Service..." -ForegroundColor Gray
        $title = "SupplySight - Tracking Service (8083)"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = '$title'; cd '$PSScriptRoot'; .\start-tracking-service.ps1"
    }

    # Visibility Service
    if (Test-PortInUse -Port $servicePorts["Visibility"]) {
        Write-Host "   Visibility Service already running on port $($servicePorts["Visibility"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        Write-Host "   Launching Visibility Service..." -ForegroundColor Gray
        $title = "SupplySight - Visibility Service (8084)"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = '$title'; cd '$PSScriptRoot'; .\start-visibility-service.ps1"
    }

    # Prediction Service
    if (Test-PortInUse -Port $servicePorts["Prediction"]) {
        Write-Host "   Prediction Service already running on port $($servicePorts["Prediction"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        Write-Host "   Launching Prediction Service..." -ForegroundColor Gray
        $title = "SupplySight - Prediction Service (8085)"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = '$title'; cd '$PSScriptRoot'; .\start-prediction-service.ps1"
    }

    # 3. Start Frontend
    Write-Host ""
    Write-Host "3. Starting Frontend..." -ForegroundColor Yellow
    if (Test-PortInUse -Port $servicePorts["Frontend"]) {
        Write-Host "   Frontend already running on port $($servicePorts["Frontend"]). Skipping." -ForegroundColor DarkYellow
    }
    else {
        $title = "SupplySight - Frontend (5173)"
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "`$Host.UI.RawUI.WindowTitle = '$title'; cd '$PSScriptRoot'; .\start-frontend.ps1"
    }

    # 4. Verify Data (Async)
    Write-Host ""
    Write-Host "4. Verifying Data..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-Command", "cd '$PSScriptRoot'; .\seed-data.ps1"

    # Summary
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  All services have been launched!     " -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
}

Write-Host ""
Write-Host "Frontend: http://localhost:5173" -ForegroundColor Cyan
Write-Host "Login:    admin@demo.com / admin123" -ForegroundColor Cyan
Write-Host ""
Write-Host "Tip: Run .\status.ps1 to check service status" -ForegroundColor DarkGray
Write-Host "     Run .\stop-all.ps1 to stop all services" -ForegroundColor DarkGray
Write-Host ""

