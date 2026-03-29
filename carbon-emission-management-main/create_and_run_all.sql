-- Carbon Emission Analysis & Business Carbon Savings Simulator
-- Script to create all database objects and test them

-- First, drop any existing objects in the correct order to avoid dependency issues
DROP TRIGGER IF EXISTS after_simulation_insert;
DROP TRIGGER IF EXISTS after_user_login_update;
DROP PROCEDURE IF EXISTS update_business_emission_stats;
DROP PROCEDURE IF EXISTS generate_simulation_recommendations;
DROP FUNCTION IF EXISTS calculate_emissions;
DROP FUNCTION IF EXISTS calculate_available_incentives;

-- ===== CREATE FUNCTIONS =====

-- 1. Function to calculate emissions based on energy source and consumption
DELIMITER //
CREATE FUNCTION calculate_emissions(source_id_param INT, consumption_kwh DECIMAL(15,2))
RETURNS DECIMAL(15,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE carbon_factor_value DECIMAL(10,4);
    DECLARE total_emissions DECIMAL(15,2);
    
    -- Get the carbon factor for the specified energy source
    SELECT carbon_factor INTO carbon_factor_value
    FROM EnergySource
    WHERE source_id = source_id_param;
    
    -- Calculate emissions
    SET total_emissions = consumption_kwh * carbon_factor_value;
    
    RETURN total_emissions;
END //
DELIMITER ;

-- 2. Function to calculate available incentives for renewable energy adoption
DELIMITER //
CREATE FUNCTION calculate_available_incentives(
    business_id_param INT, 
    source_id_param INT, 
    investment_amount DECIMAL(15,2)
)
RETURNS DECIMAL(15,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE incentive_total DECIMAL(15,2) DEFAULT 0;
    DECLARE business_size VARCHAR(50);
    DECLARE source_type VARCHAR(50);
    DECLARE is_renewable BOOLEAN;
    
    -- Get business size
    SELECT size INTO business_size
    FROM Business
    WHERE business_id = business_id_param;
    
    -- Get source information
    SELECT source_name, is_renewable INTO source_type, is_renewable
    FROM EnergySource
    WHERE source_id = source_id_param;
    
    -- Calculate incentives based on source type, business size, and investment
    IF is_renewable = TRUE THEN
        -- Base incentive percentage based on source type
        IF source_type = 'Solar' THEN
            SET incentive_total = investment_amount * 0.30; -- 30% for solar
        ELSEIF source_type = 'Wind' THEN
            SET incentive_total = investment_amount * 0.25; -- 25% for wind
        ELSEIF source_type = 'Hydro' THEN
            SET incentive_total = investment_amount * 0.20; -- 20% for hydro
        ELSE
            SET incentive_total = investment_amount * 0.15; -- 15% for other renewables
        END IF;
        
        -- Additional incentives based on business size
        IF business_size = 'Small' THEN
            SET incentive_total = incentive_total + (investment_amount * 0.10); -- Extra 10% for small businesses
        ELSEIF business_size = 'Medium' THEN
            SET incentive_total = incentive_total + (investment_amount * 0.05); -- Extra 5% for medium businesses
        END IF;
    ELSE
        -- No incentives for non-renewable energy sources
        SET incentive_total = 0;
    END IF;
    
    RETURN incentive_total;
END //
DELIMITER ;

-- ===== CREATE PROCEDURES =====

-- 1. Procedure to update business emission statistics based on simulations
DELIMITER //
CREATE PROCEDURE update_business_emission_stats(IN business_id_param INT, IN return_message BOOLEAN)
BEGIN
    DECLARE avg_current_emissions DECIMAL(15,2);
    DECLARE avg_target_emissions DECIMAL(15,2);
    DECLARE avg_cost_savings DECIMAL(15,2);
    DECLARE avg_payback_period DECIMAL(10,2);
    DECLARE total_simulations INT;
    DECLARE business_exists INT;
    
    -- Calculate statistics from simulations
    SELECT 
        AVG(current_total_emissions),
        AVG(target_total_emissions),
        AVG(estimated_cost_savings),
        AVG(payback_period_months),
        COUNT(*)
    INTO 
        avg_current_emissions,
        avg_target_emissions,
        avg_cost_savings,
        avg_payback_period,
        total_simulations
    FROM 
        EmissionSimulation
    WHERE 
        business_id = business_id_param;
    
    -- Check if the business exists in the stats table
    SELECT COUNT(*) INTO business_exists
    FROM BusinessEmissionStats
    WHERE business_id = business_id_param;
    
    -- Update or insert based on existence
    IF business_exists > 0 THEN
        UPDATE BusinessEmissionStats
        SET 
            avg_current_emissions = avg_current_emissions,
            avg_target_emissions = avg_target_emissions,
            potential_reduction_percentage = 
                CASE 
                    WHEN avg_current_emissions > 0 
                    THEN ((avg_current_emissions - avg_target_emissions) / avg_current_emissions) * 100 
                    ELSE 0 
                END,
            avg_cost_savings = avg_cost_savings,
            avg_payback_period_months = avg_payback_period,
            total_simulations = total_simulations,
            last_updated = NOW()
        WHERE 
            business_id = business_id_param;
    ELSE
        INSERT INTO BusinessEmissionStats (
            business_id,
            avg_current_emissions,
            avg_target_emissions,
            potential_reduction_percentage,
            avg_cost_savings,
            avg_payback_period_months,
            total_simulations,
            last_updated
        ) VALUES (
            business_id_param,
            avg_current_emissions,
            avg_target_emissions,
            CASE 
                WHEN avg_current_emissions > 0 
                THEN ((avg_current_emissions - avg_target_emissions) / avg_current_emissions) * 100 
                ELSE 0 
            END,
            avg_cost_savings,
            avg_payback_period,
            total_simulations,
            NOW()
        );
    END IF;
    
    -- Only return a message if return_message is TRUE
    IF return_message = TRUE THEN
        SELECT 
            CONCAT('Business ID ', business_id_param, ' emission stats updated successfully. ',
                  'Average reduction: ', 
                  ROUND(((avg_current_emissions - avg_target_emissions) / avg_current_emissions) * 100, 2),
                  '%, Based on ', total_simulations, ' simulations.') AS result;
    END IF;
END //
DELIMITER ;

-- 2. Procedure to generate recommendations based on simulation data
DELIMITER //
CREATE PROCEDURE generate_simulation_recommendations(IN business_id_param INT)
BEGIN
    DECLARE avg_current_emissions DECIMAL(15,2);
    DECLARE business_industry VARCHAR(100);
    
    -- Get business industry
    SELECT industry INTO business_industry
    FROM Business
    WHERE business_id = business_id_param;
    
    -- Get average current emissions
    SELECT AVG(current_total_emissions) INTO avg_current_emissions
    FROM EmissionSimulation
    WHERE business_id = business_id_param;
    
    -- Generate recommendations based on industry and emission levels
    SELECT 
        CASE 
            WHEN business_industry = 'Manufacturing' THEN
                CASE 
                    WHEN avg_current_emissions > 2000000 THEN 'High emission manufacturing facility. Recommend immediate implementation of solar panels, waste heat recovery systems, and energy-efficient machinery upgrades.'
                    WHEN avg_current_emissions > 1000000 THEN 'Medium emission manufacturing facility. Recommend phased implementation of solar panels and energy-efficient machinery.'
                    ELSE 'Low emission manufacturing facility. Recommend energy audit and targeted efficiency improvements.'
                END
            WHEN business_industry = 'Technology' THEN
                CASE 
                    WHEN avg_current_emissions > 1000000 THEN 'High energy data center. Recommend server virtualization, cooling optimization, and 100% renewable energy procurement.'
                    WHEN avg_current_emissions > 500000 THEN 'Medium energy technology operation. Recommend partial renewable energy procurement and equipment upgrades.'
                    ELSE 'Energy-efficient technology operation. Recommend maintaining current practices with minor optimizations.'
                END
            WHEN business_industry = 'Healthcare' THEN
                CASE 
                    WHEN avg_current_emissions > 1500000 THEN 'Large healthcare facility with high emissions. Recommend HVAC modernization, LED lighting, and on-site renewable generation.'
                    WHEN avg_current_emissions > 750000 THEN 'Medium-sized healthcare facility. Recommend building management system upgrades and partial renewable implementation.'
                    ELSE 'Small or efficient healthcare facility. Recommend equipment timer installation and lighting upgrades.'
                END
            ELSE 
                CASE 
                    WHEN avg_current_emissions > 1000000 THEN 'High emission business. Recommend comprehensive energy audit and full renewable transition plan.'
                    WHEN avg_current_emissions > 500000 THEN 'Medium emission business. Recommend targeted efficiency improvements in highest consumption areas.'
                    ELSE 'Low emission business. Recommend maintaining current practices with regular monitoring.'
                END
        END AS recommendation,
        
        CASE 
            WHEN avg_current_emissions > 1000000 THEN 'High Priority'
            WHEN avg_current_emissions > 500000 THEN 'Medium Priority'
            ELSE 'Low Priority'
        END AS priority,
        
        CONCAT('Based on ', 
               (SELECT COUNT(*) FROM EmissionSimulation WHERE business_id = business_id_param), 
               ' simulations with average emissions of ', 
               ROUND(avg_current_emissions, 2), 
               ' kg CO2e') AS analysis_basis
    ;
END //
DELIMITER ;

-- ===== CREATE TRIGGERS =====

-- 1. Trigger to update business emission stats after a new simulation is inserted
DELIMITER //
CREATE TRIGGER after_simulation_insert
AFTER INSERT ON EmissionSimulation
FOR EACH ROW
BEGIN
    -- Call the procedure with FALSE to prevent result set from being returned
    CALL update_business_emission_stats(NEW.business_id, FALSE);
END //
DELIMITER ;

-- 2. Trigger to track user login history
DELIMITER //
CREATE TRIGGER after_user_login_update
AFTER UPDATE ON User
FOR EACH ROW
BEGIN
    -- If the last_login field was updated
    IF NEW.last_login != OLD.last_login THEN
        -- Insert a record into login history
        INSERT INTO LoginHistory (user_id, login_time)
        VALUES (NEW.user_id, NEW.last_login);
    END IF;
END //
DELIMITER ;

-- ===== VERIFY CREATION =====

-- Show all created procedures
SHOW PROCEDURE STATUS WHERE Db = DATABASE();

-- Show all created functions
SHOW FUNCTION STATUS WHERE Db = DATABASE();

-- Show all created triggers
SHOW TRIGGERS;

-- ===== RUN THE DEMO SCRIPT =====

SOURCE run_plsql.sql; 