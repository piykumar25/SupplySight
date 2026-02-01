-- SupplySight Alert Seeding Script
-- Can be run via: docker exec -i supplysight-postgres psql -U supplysight -d supplysight < seed_alerts_data.sql

DO $$
DECLARE
    v_tenant_id UUID;
    v_user_id UUID;
    v_shipment_id UUID;
    v_pred1 UUID := gen_random_uuid();
    v_pred2 UUID := gen_random_uuid();
    v_pred3 UUID := gen_random_uuid();
    v_pred4 UUID := gen_random_uuid();
    v_pred5 UUID := gen_random_uuid();
BEGIN
    -- Get the correct tenant ID for the demo admin user
    SELECT tenant_id INTO v_tenant_id FROM identity.users WHERE email = 'admin@demo.com';
    SELECT id INTO v_user_id FROM identity.users WHERE email = 'admin@demo.com';
    
    -- If no tenant/user, we can't seed properly
    IF v_tenant_id IS NULL THEN 
        RAISE NOTICE 'No tenant found for admin@demo.com, skipping alert seed';
        RETURN;
    END IF;

    -- Create Dummy Predictions (Required for FK constraint)
    -- We use arbitrary shipment IDs since Prediction Service decouples them from Tracking Service DB
    
    INSERT INTO prediction.shipment_predictions (
        id, tenant_id, shipment_id, event_id, delay_probability, delay_risk, 
        anomaly_detected, created_at
    ) VALUES 
    (v_pred1, v_tenant_id, gen_random_uuid(), gen_random_uuid(), 0.8, 'HIGH', false, NOW()),
    (v_pred2, v_tenant_id, gen_random_uuid(), gen_random_uuid(), 0.5, 'MEDIUM', true, NOW()),
    (v_pred3, v_tenant_id, gen_random_uuid(), gen_random_uuid(), 0.2, 'LOW', false, NOW()),
    (v_pred4, v_tenant_id, gen_random_uuid(), gen_random_uuid(), 0.9, 'HIGH', true, NOW()),
    (v_pred5, v_tenant_id, gen_random_uuid(), gen_random_uuid(), 0.1, 'LOW', false, NOW());


    -- Create Alerts (Linking to predictions)
    
    -- 1. CRITICAL Delay Risk
    INSERT INTO prediction.alerts (
        id, tenant_id, shipment_id, prediction_id, alert_type, severity, message, 
        details, acknowledged, created_at, resolved
    ) VALUES (
        gen_random_uuid(), v_tenant_id, gen_random_uuid(), v_pred1, 
        'DELAY_RISK_HIGH', 'CRITICAL', 'Shipment TRK-001 is at high risk of delay due to weather conditions',
        '{"location": {"lat": 40.7128, "lng": -74.0060}, "speed": 0}', false, NOW(), false
    );

    -- 2. WARNING Route Deviation
    INSERT INTO prediction.alerts (
        id, tenant_id, shipment_id, prediction_id, alert_type, severity, message, 
        details, acknowledged, created_at, resolved
    ) VALUES (
        gen_random_uuid(), v_tenant_id, gen_random_uuid(), v_pred2, 
        'ROUTE_DEVIATION', 'WARNING', 'Shipment TRK-005 deviated from planned route by 50km',
        '{"deviation_km": 50, "location": {"lat": 34.0522, "lng": -118.2437}}', false, NOW() - INTERVAL '2 hours', false
    );

    -- 3. INFO ETA Slip
    INSERT INTO prediction.alerts (
        id, tenant_id, shipment_id, prediction_id, alert_type, severity, message, 
        details, acknowledged, created_at, resolved
    ) VALUES (
        gen_random_uuid(), v_tenant_id, gen_random_uuid(), v_pred3, 
        'ETA_SLIP', 'INFO', 'ETA updated for TRK-003, delayed by 4 hours',
        '{"original_eta": "2023-10-10T10:00:00Z", "new_eta": "2023-10-10T14:00:00Z"}', true, NOW() - INTERVAL '1 day', false
    );

     -- 4. RESOLVED Anomaly
    INSERT INTO prediction.alerts (
        id, tenant_id, shipment_id, prediction_id, alert_type, severity, message, 
        details, acknowledged, created_at, resolved, resolved_at, resolved_by, resolution_comment, acknowledged_at, acknowledged_by
    ) VALUES (
        gen_random_uuid(), v_tenant_id, gen_random_uuid(), v_pred4, 
        'ANOMALY_DETECTED', 'CRITICAL', 'Temperature anomaly detected in container',
        '{"temp_c": 25, "threshold_c": 20}', true, NOW() - INTERVAL '2 days', true, NOW() - INTERVAL '1 day', v_user_id, 'Sensor recalibrated', NOW() - INTERVAL '2 days', v_user_id
    );

    -- 5. INFO Speed Anomaly
    INSERT INTO prediction.alerts (
        id, tenant_id, shipment_id, prediction_id, alert_type, severity, message, 
        details, acknowledged, created_at, resolved
    ) VALUES (
        gen_random_uuid(), v_tenant_id, gen_random_uuid(), v_pred5, 
        'SPEED_ANOMALY', 'INFO', 'Shipment TRK-004 speed exceeds average by 20%',
        '{"speed": 120, "avg_speed": 100}', false, NOW() - INTERVAL '30 minutes', false
    );

END $$;
