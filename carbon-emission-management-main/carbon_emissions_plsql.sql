USE carbon_emissions_db;

-- ============== PL/SQL PROCEDURES, FUNCTIONS, AND TRIGGERS ==============

-- ===== PROCEDURES =====

-- Procedure 1: Calculate and update business emission statistics
DELIMITER //
CREATE PROCEDURE update_business_emission_stats(IN business_id_param INT, IN return_message BOOLEAN DEFAULT FALSE)
BEGIN
    DECLARE simulation_count INT;
    DECLARE avg_reduction DECIMAL(20,2);
    DECLARE max_savings DECIMAL(20,2);

    -- Calculate statistics from simulation data
    SELECT 
        COUNT(simulation_id),
        AVG(current_total_emissions - target_total_emissions),
        MAX(estimated_cost_savings)
    INTO
        simulation_count,
        avg_reduction,
        max_savings
    FROM 
        EmissionSimulation
    WHERE 
        business_id = business_id_param;

    -- If business exists in stats table, update it
    IF EXISTS (SELECT 1 FROM BusinessEmissionStats WHERE business_id = business_id_param) THEN
        UPDATE BusinessEmissionStats
        SET 
            total_simulations = simulation_count,
            avg_emissions_reduction = avg_reduction,
            max_cost_savings = max_savings,
            last_updated = NOW()
        WHERE 
            business_id = business_id_param;
    ELSE
        -- If business doesn't exist in stats table, insert new record
        INSERT INTO BusinessEmissionStats (
            business_id, 
            total_simulations, 
            avg_emissions_reduction, 
            max_cost_savings, 
            last_updated
        ) VALUES (
            business_id_param,
            simulation_count,
            avg_reduction,
            max_savings,
            NOW()
        );
    END IF;
    
    -- Only return a message if explicitly requested (not from a trigger)
    IF return_message THEN
        SELECT CONCAT('Statistics updated for business ID: ', business_id_param) AS message;
    END IF;
END //
DELIMITER ;

-- Procedure 2: Generate simulation report with recommendations
DELIMITER //
CREATE PROCEDURE generate_simulation_recommendations(IN simulation_id_param INT)
BEGIN
    DECLARE business_name_var VARCHAR(100);
    DECLARE current_emissions DECIMAL(20,2);
    DECLARE target_emissions DECIMAL(20,2);
    DECLARE cost_savings DECIMAL(20,2);
    DECLARE payback_period INT;
    DECLARE reduction_percentage DECIMAL(10,2);
    DECLARE recommendation_text TEXT;
    
    -- Get simulation details
    SELECT 
        b.business_name,
        es.current_total_emissions,
        es.target_total_emissions,
        es.estimated_cost_savings,
        es.payback_period_months
    INTO 
        business_name_var,
        current_emissions,
        target_emissions,
        cost_savings,
        payback_period
    FROM 
        EmissionSimulation es
    JOIN 
        Business b ON es.business_id = b.business_id
    WHERE 
        es.simulation_id = simulation_id_param;
    
    -- Calculate reduction percentage
    SET reduction_percentage = ((current_emissions - target_emissions) / current_emissions) * 100;
    
    -- Generate recommendation based on results
    IF reduction_percentage >= 40 THEN
        SET recommendation_text = CONCAT(
            'EXCELLENT REDUCTION POTENTIAL: ', business_name_var, ' can achieve a significant ',
            ROUND(reduction_percentage, 2), '% reduction in carbon emissions. ',
            'The estimated annual cost savings of $', format(cost_savings, 2), ' provide an attractive ',
            'return on investment with a payback period of ', payback_period, ' months. ',
            'Recommended actions: Proceed with full implementation plan while exploring additional incentives.'
        );
    ELSEIF reduction_percentage >= 20 THEN
        SET recommendation_text = CONCAT(
            'GOOD REDUCTION POTENTIAL: ', business_name_var, ' can achieve a substantial ',
            ROUND(reduction_percentage, 2), '% reduction in carbon emissions. ',
            'The estimated annual cost savings of $', format(cost_savings, 2), ' with a payback period of ',
            payback_period, ' months offers a reasonable business case. ',
            'Recommended actions: Implement in phases, starting with highest ROI transitions.'
        );
    ELSE
        SET recommendation_text = CONCAT(
            'MODERATE REDUCTION POTENTIAL: ', business_name_var, ' can achieve a ',
            ROUND(reduction_percentage, 2), '% reduction in carbon emissions. ',
            'The estimated annual cost savings of $', format(cost_savings, 2), ' with a payback period of ',
            payback_period, ' months may be improved by exploring additional options. ',
            'Recommended actions: Consider alternative energy sources or improved efficiency measures.'
        );
    END IF;
    
    -- Output recommendation
    SELECT recommendation_text AS simulation_recommendation;
    
    -- Also display detailed breakdown of changes
    SELECT 
        es_current.source_name AS current_source,
        es_new.source_name AS proposed_source,
        sd.energy_amount,
        sd.emissions_reduction,
        ROUND((sd.emissions_reduction / sd.energy_amount), 4) AS reduction_per_unit,
        sd.implementation_cost,
        sd.annual_savings,
        ROUND((sd.annual_savings / sd.implementation_cost) * 100, 2) AS roi_percentage
    FROM 
        SimulationDetail sd
    JOIN 
        EnergySource es_current ON sd.current_source_id = es_current.source_id
    JOIN 
        EnergySource es_new ON sd.new_source_id = es_new.source_id
    WHERE 
        sd.simulation_id = simulation_id_param
    ORDER BY 
        roi_percentage DESC;
END //
DELIMITER ;

-- ===== FUNCTIONS =====

-- Function 1: Calculate carbon emissions based on energy source and usage
DELIMITER //
CREATE FUNCTION calculate_emissions(source_id_param INT, energy_amount_param DECIMAL(20,2))
RETURNS DECIMAL(20,2)
DETERMINISTIC
BEGIN
    DECLARE carbon_factor DECIMAL(10,4);
    DECLARE emissions DECIMAL(20,2);
    
    -- Get carbon factor for the energy source
    SELECT carbon_factor INTO carbon_factor 
    FROM EnergySource 
    WHERE source_id = source_id_param;
    
    -- Calculate emissions
    SET emissions = energy_amount_param * carbon_factor;
    
    RETURN emissions;
END //
DELIMITER ;

-- Function 2: Calculate potential incentive amount for a renewable energy transition
DELIMITER //
CREATE FUNCTION calculate_available_incentives(
    country_id_param INT, 
    source_id_param INT, 
    implementation_cost_param DECIMAL(20,2)
)
RETURNS DECIMAL(20,2)
READS SQL DATA
BEGIN
    DECLARE total_incentive DECIMAL(20,2) DEFAULT 0;
    DECLARE source_id_str VARCHAR(10);
    DECLARE incentive_amount DECIMAL(20,2);
    DECLARE done BOOLEAN DEFAULT FALSE;
    DECLARE found_incentives CURSOR FOR
        SELECT 
            LEAST(
                (amount_percentage / 100) * implementation_cost_param,
                max_amount
            ) AS incentive_amount
        FROM 
            Incentive
        WHERE 
            country_id = country_id_param
            AND start_date <= CURDATE()
            AND (end_date IS NULL OR end_date >= CURDATE())
            AND (
                applicable_sources LIKE CONCAT('%', source_id_str, '%')
                OR applicable_sources LIKE CONCAT(source_id_str, ',%')
                OR applicable_sources LIKE CONCAT('%,', source_id_str)
                OR applicable_sources = source_id_str
            );
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SET source_id_str = CAST(source_id_param AS CHAR);
    
    -- Calculate total applicable incentives
    OPEN found_incentives;
    
    incentive_loop: LOOP
        FETCH found_incentives INTO incentive_amount;
        
        IF done THEN
            LEAVE incentive_loop;
        END IF;
        
        SET total_incentive = total_incentive + incentive_amount;
    END LOOP;
    
    CLOSE found_incentives;
    
    RETURN total_incentive;
END //
DELIMITER ;

-- ===== TRIGGERS =====

-- Trigger 1: Update business emission stats when a new simulation is added
DELIMITER //
CREATE TRIGGER after_simulation_insert
AFTER INSERT ON EmissionSimulation
FOR EACH ROW
BEGIN
    -- Call the procedure to update business emission stats with FALSE to not return a message
    CALL update_business_emission_stats(NEW.business_id, FALSE);
END //
DELIMITER ;

-- Trigger 2: Record user login information automatically
DELIMITER //
CREATE TRIGGER after_user_login_update
AFTER UPDATE ON User
FOR EACH ROW
BEGIN
    -- If last_login field has been updated, record a new login history entry
    IF NEW.last_login IS NOT NULL AND (OLD.last_login IS NULL OR NEW.last_login > OLD.last_login) THEN
        INSERT INTO LoginHistory (
            user_id,
            login_time,
            ip_address,
            device_info
        ) VALUES (
            NEW.user_id,
            NEW.last_login,
            '127.0.0.1', -- This would be replaced with actual IP in a real application
            'System recorded login' -- This would be replaced with actual device info
        );
    END IF;
END //
DELIMITER ;

-- Example procedure call
-- CALL update_business_emission_stats(1, TRUE);
-- CALL generate_simulation_recommendations(1);

-- Example function call
-- SELECT calculate_emissions(1, 1000000.00) AS coal_emissions;
-- SELECT calculate_available_incentives(1, 3, 500000.00) AS available_solar_incentives; 