# SupplySight - Start All Services (Background Mode)
# This script starts the entire stack in background jobs to keep your terminal clean

Write-Host "Starting SupplySight Stack in Background..." -ForegroundColor Cyan

# 1. Start Infrastructure (Docker)
Write-Host "1. Checking Infrastructure..." -ForegroundColor Yellow
$dockerStatus = docker-compose -f ../infra/docker-compose.yml ps -q
if (-not $dockerStatus) {
    Write-Host "   Starting Docker containers..." -ForegroundColor Gray
    Start-Process powershell -ArgumentList "-Command", "cd ../infra; docker-compose up -d" -Wait
    Write-Host "   Waiting 30s for databases to initialize..." -ForegroundColor Gray
    Start-Sleep -Seconds 30
} else {
    Write-Host "   Infrastructure is already running." -ForegroundColor Green
}

# Function to start a service in background
function Start-ServiceBackground {
    param (
        [string]$ServiceName,
        [string]$ScriptPath,
        [string]$LogFile
    )
    Write-Host "   Launching $ServiceName..." -ForegroundColor Gray
    
    $jobScript = {
        param($path)
        Set-Location -Path $path
        # We need to extract the actual command from the ps1 file or run it directly
        # Running the ps1 directly is easiest but might have path issues if not careful
        # Let's run the maven command directly for reliability in background jobs
        
        # Sourcing env vars from the script is tricky in a job block. 
        # Better approach: Start-Process with -WindowStyle Hidden
    }
    
    # Using Start-Process with WindowStyle Hidden is better than Start-Job for this
    # because it keeps the process alive even if this script closes, but hides the window.
    Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./$ScriptPath > $LogFile 2>&1"
    Write-Host "   Started $ServiceName (Logs: $LogFile)" -ForegroundColor Green
}

# 2. Start Backend Services
Write-Host "2. Starting Backend Services (Hidden Windows)..." -ForegroundColor Yellow

# Create logs directory
New-Item -ItemType Directory -Force -Path "logs" | Out-Null

# Identity Service
Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-identity-service.ps1 > logs/identity-service.log 2>&1"
Write-Host "   Started Identity Service" -ForegroundColor Green

# Wait a bit
Start-Sleep -Seconds 5

# Tracking Service
Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-tracking-service.ps1 > logs/tracking-service.log 2>&1"
Write-Host "   Started Tracking Service" -ForegroundColor Green

# Visibility Service
Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-visibility-service.ps1 > logs/visibility-service.log 2>&1"
Write-Host "   Started Visibility Service" -ForegroundColor Green

# Prediction Service
Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-prediction-service.ps1 > logs/prediction-service.log 2>&1"
Write-Host "   Started Prediction Service" -ForegroundColor Green

# 3. Start Frontend
Write-Host "3. Starting Frontend (Hidden Window)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-WindowStyle", "Hidden", "-Command", "cd '$PSScriptRoot'; ./start-frontend.ps1 > logs/frontend.log 2>&1"
Write-Host "   Started Frontend" -ForegroundColor Green

Write-Host ""
Write-Host "All services represent running in the background!" -ForegroundColor Cyan
Write-Host "Logs are being written to the 'tools/logs' directory." -ForegroundColor Cyan
Write-Host "To stop them, use Task Manager or run 'Stop-Process -Name java,node'" -ForegroundColor Yellow
Write-Host "Frontend will be available at http://localhost:5173" -ForegroundColor Green
