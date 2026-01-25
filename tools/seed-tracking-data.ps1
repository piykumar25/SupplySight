# SupplySight - Seed Tracking Data
# This script injects dummy shipment data into the Tracking Service

Write-Host "Seeding shipment data..." -ForegroundColor Cyan

# 1. Login to get Token
$loginUrl = "http://localhost:8081/api/v1/login"
$body = @{
    email    = "admin@demo.com"
    password = "admin123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method POST -Body $body -ContentType "application/json"
    $token = $loginResponse.accessToken
    Write-Host "Got Authentication Token" -ForegroundColor Green
}
catch {
    Write-Host "Failed to login. Ensure Identity Service is running." -ForegroundColor Red
    Write-Host $_
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

# 2. Define Shipments
$shipments = @(
    @{
        trackingNumber    = "TRK-001"
        origin            = "New York, NY"
        destination       = "Los Angeles, CA"
        currentStatus     = "IN_TRANSIT"
        estimatedDelivery = (Get-Date).AddDays(3).ToString("yyyy-MM-ddTHH:mm:ss")
    },
    @{
        trackingNumber    = "TRK-002"
        origin            = "Chicago, IL"
        destination       = "Miami, FL"
        currentStatus     = "DELIVERED"
        estimatedDelivery = (Get-Date).AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ss")
    },
    @{
        trackingNumber    = "TRK-003"
        origin            = "Seattle, WA"
        destination       = "Houston, TX"
        currentStatus     = "PICKED_UP"
        estimatedDelivery = (Get-Date).AddDays(5).ToString("yyyy-MM-ddTHH:mm:ss")
    },
    @{
        trackingNumber    = "TRK-004"
        origin            = "Boston, MA"
        destination       = "San Francisco, CA"
        currentStatus     = "DELAYED"
        estimatedDelivery = (Get-Date).AddDays(4).ToString("yyyy-MM-ddTHH:mm:ss")
    },
    @{
        trackingNumber    = "TRK-005"
        origin            = "Denver, CO"
        destination       = "Austin, TX"
        currentStatus     = "IN_TRANSIT"
        estimatedDelivery = (Get-Date).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
    }
)

# 3. Post Shipments
foreach ($shipment in $shipments) {
    $url = "http://localhost:8083/api/v1/shipments"
    $json = $shipment | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri $url -Method POST -Headers $headers -Body $json
        Write-Host "Created Shipment $($shipment.trackingNumber)" -ForegroundColor Green
    }
    catch {
        Write-Host "Failed to create shipment $($shipment.trackingNumber): $_" -ForegroundColor Red
    }
}

# 4. Add Events
Write-Host "Adding initial events..." -ForegroundColor Cyan
$events = @(
    @{
        trackingNumber = "TRK-001"
        status         = "PICKED_UP"
        location       = "New York, NY"
        description    = "Package picked up from warehouse"
        timestamp      = (Get-Date).AddDays(-2).ToString("yyyy-MM-ddTHH:mm:ss")
    },
    @{
        trackingNumber = "TRK-001"
        status         = "IN_TRANSIT"
        location       = "Cleveland, OH"
        description    = "Arrived at sorting facility"
        timestamp      = (Get-Date).AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ss")
    },
    @{
        trackingNumber = "TRK-002"
        status         = "DELIVERED"
        location       = "Miami, FL"
        description    = "Delivered to recipient"
        timestamp      = (Get-Date).AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ss")
    }
)

foreach ($event in $events) {
    $url = "http://localhost:8083/api/v1/shipments/$($event.trackingNumber)/events"
    $json = $event | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri $url -Method POST -Headers $headers -Body $json
        Write-Host "Added Event to $($event.trackingNumber): $($event.status)" -ForegroundColor Green
    }
    catch {
        Write-Host "Failed to add event: $_" -ForegroundColor Red
    }
}

Write-Host "Done! Data seeded." -ForegroundColor Green
