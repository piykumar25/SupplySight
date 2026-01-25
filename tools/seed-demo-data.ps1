# SupplySight - Seed Demo Data
# This script seeds demo data after Identity Service is running

Write-Host "Seeding demo data..." -ForegroundColor Green

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/seed/demo" -Method POST -ContentType "application/json"
    Write-Host "Demo data seeded successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Demo Credentials:" -ForegroundColor Cyan
    Write-Host "  Admin:   admin@demo.com / admin123"
    Write-Host "  Ops:     ops@demo.com / ops123"
    Write-Host "  Viewer:  viewer@demo.com / viewer123"
} catch {
    Write-Host "Failed to seed demo data. Make sure Identity Service is running on port 8081." -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
}
