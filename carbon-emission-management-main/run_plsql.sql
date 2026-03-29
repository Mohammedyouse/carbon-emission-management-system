-- Carbon Emission Analysis & Business Carbon Savings Simulator
-- Demo script to execute all procedures, functions, and triggers

-- ===== DEMONSTRATE TRIGGERS =====

-- 1. after_simulation_insert trigger
-- (This will automatically update business stats through the trigger)
INSERT INTO EmissionSimulation (
    business_id, 
    user_id, 
    simulation_name, 
    current_total_emissions, 
    target_total_emissions, 
    estimated_cost_savings, 
    payback_period_months, 
    notes
) VALUES (
    1, 4, 'Solar Expansion Plan', 
    1500000.00, 1200000.00, 
    125000.00, 36, 
    'Transition 30% of coal usage to solar'
);

-- Confirm the simulation was inserted
SELECT * FROM EmissionSimulation WHERE simulation_name = 'Solar Expansion Plan';

-- Confirm business stats were updated by the trigger
SELECT * FROM BusinessEmissionStats WHERE business_id = 1;

-- 2. after_user_login_update trigger
-- (This will automatically create a login history entry)
UPDATE User
SET last_login = NOW()
WHERE user_id = 5;

-- Confirm the login history entry was created
SELECT * FROM LoginHistory WHERE user_id = 5 ORDER BY login_time DESC LIMIT 1;

-- ===== DEMONSTRATE PROCEDURES =====

-- 1. update_business_emission_stats Procedure
-- (Note: added TRUE parameter to see the result message)
CALL update_business_emission_stats(1, TRUE);

-- 2. generate_simulation_recommendations Procedure
CALL generate_simulation_recommendations(3);

-- ===== DEMONSTRATE FUNCTIONS =====

-- 1. calculate_emissions Function
SELECT calculate_emissions(1, 1000000.00) AS coal_emissions;
SELECT calculate_emissions(3, 1000000.00) AS solar_emissions;
SELECT calculate_emissions(4, 1000000.00) AS wind_emissions;

-- Show emissions comparison for different energy sources
SELECT 
    source_name,
    is_renewable,
    carbon_factor,
    calculate_emissions(source_id, 1000000.00) AS emissions_per_million_kwh
FROM
    EnergySource
ORDER BY 
    emissions_per_million_kwh DESC;

-- 2. calculate_available_incentives Function
SELECT calculate_available_incentives(1, 3, 500000.00) AS available_solar_incentives;

-- Show incentives comparison for different renewable sources
SELECT 
    es.source_name,
    es.is_renewable,
    calculate_available_incentives(1, es.source_id, 500000.00) AS available_incentives
FROM
    EnergySource es
WHERE
    es.is_renewable = TRUE
ORDER BY 
    available_incentives DESC;

