# SupplySight - Start Frontend
# This script starts the React frontend dev server

Write-Host "Starting SupplySight Frontend..." -ForegroundColor Green

Set-Location -Path "$PSScriptRoot\..\supplysight-web"
npm run dev
