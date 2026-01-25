# SupplySight - Quick Start Guide
# ================================
# 
# This folder contains scripts to start all SupplySight services.
#
# PREREQUISITES:
# 1. Docker infrastructure must be running:
#    cd e:\SupplySight\infra
#    docker-compose up -d
#
# 2. Maven project must be built (run once):
#    cd e:\SupplySight
#    mvn clean install -DskipTests
#
# STARTUP ORDER:
# ==============
# Open a new PowerShell terminal for each service:
#
# Terminal 1 - Identity Service (Required for login):
#    .\start-identity-service.ps1
#
# Terminal 2 - Tracking Service:
#    .\start-tracking-service.ps1
#
# Terminal 3 - Visibility Service:
#    .\start-visibility-service.ps1
#
# Terminal 4 - Prediction Service:
#    .\start-prediction-service.ps1
#
# Terminal 5 - Frontend:
#    .\start-frontend.ps1
#
# AFTER IDENTITY SERVICE STARTS:
# ==============================
# Seed demo data (run once):
#    .\seed-demo-data.ps1
#
# DEMO CREDENTIALS:
# =================
# Admin:   admin@demo.com / admin123
# Ops:     ops@demo.com / ops123  
# Viewer:  viewer@demo.com / viewer123
#
# SERVICE PORTS:
# ==============
# Identity Service:   http://localhost:8081
# Tracking Service:   http://localhost:8083
# Visibility Service: http://localhost:8084
# Prediction Service: http://localhost:8085
# Frontend:           http://localhost:5173
