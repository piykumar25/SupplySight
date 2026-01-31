# SupplySight - Service Status Check
# This script checks the status of all SupplySight services

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    SupplySight Service Status         " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Define service ports
$services = @(
    @{ Name = "Identity Service"; Port = 8081; Type = "Backend" },
    @{ Name = "Tracking Service"; Port = 8083; Type = "Backend" },
    @{ Name = "Visibility Service"; Port = 8084; Type = "Backend" },
    @{ Name = "Prediction Service"; Port = 8085; Type = "Backend" },
    @{ Name = "Frontend"; Port = 5173; Type = "Frontend" }
)

$runningCount = 0
$totalServices = $services.Count

Write-Host "Services:" -ForegroundColor Yellow
Write-Host ""

foreach ($service in $services) {
    $connection = Get-NetTCPConnection -LocalPort $service.Port -State Listen -ErrorAction SilentlyContinue
    
    if ($connection) {
        $processId = $connection.OwningProcess | Select-Object -First 1
        $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
        $processName = if ($process) { $process.ProcessName } else { "Unknown" }
        
        Write-Host "  [OK] " -ForegroundColor Green -NoNewline
        Write-Host "$($service.Name)" -ForegroundColor White -NoNewline
        Write-Host " - Port $($service.Port) (PID: $processId, $processName)" -ForegroundColor DarkGray
        $runningCount++
    }
    else {
        Write-Host "  [--] " -ForegroundColor Red -NoNewline
        Write-Host "$($service.Name)" -ForegroundColor Gray -NoNewline
        Write-Host " - Port $($service.Port) (Not running)" -ForegroundColor DarkGray
    }
}

# Docker Infrastructure Status
Write-Host ""
Write-Host "Infrastructure (Docker):" -ForegroundColor Yellow
Write-Host ""

$dockerContainers = @(
    @{ Name = "PostgreSQL"; Container = "supplysight-postgres" },
    @{ Name = "Kafka"; Container = "supplysight-kafka" },
    @{ Name = "Redis"; Container = "supplysight-redis" },
    @{ Name = "Zookeeper"; Container = "supplysight-zookeeper" }
)

foreach ($container in $dockerContainers) {
    $status = docker inspect -f '{{.State.Status}}' $container.Container 2>$null
    
    if ($status -eq "running") {
        Write-Host "  [OK] " -ForegroundColor Green -NoNewline
        Write-Host "$($container.Name)" -ForegroundColor White -NoNewline
        Write-Host " - $($container.Container)" -ForegroundColor DarkGray
    }
    else {
        Write-Host "  [--] " -ForegroundColor Red -NoNewline
        Write-Host "$($container.Name)" -ForegroundColor Gray -NoNewline
        Write-Host " - $($container.Container) (Not running)" -ForegroundColor DarkGray
    }
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($runningCount -eq $totalServices) {
    Write-Host "  All $totalServices services are running!" -ForegroundColor Green
}
elseif ($runningCount -gt 0) {
    Write-Host "  $runningCount of $totalServices services running" -ForegroundColor Yellow
}
else {
    Write-Host "  No services are running" -ForegroundColor Red
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($runningCount -eq $totalServices) {
    Write-Host "Frontend available at: http://localhost:5173" -ForegroundColor Cyan
    Write-Host ""
}
