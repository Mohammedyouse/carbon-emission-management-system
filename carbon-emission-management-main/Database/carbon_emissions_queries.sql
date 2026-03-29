USE carbon_emissions_db;

-- =============== QUERY EXAMPLES ===============

-- 1. Basic SELECT query - Get all users with their roles
SELECT 
    u.user_id, 
    u.username, 
    u.email, 
    CONCAT(u.first_name, ' ', u.last_name) AS full_name, 
    r.role_name,
    u.is_active
FROM 
    User u
JOIN 
    Role r ON u.role_id = r.role_id;

-- 2. Aggregation - Total emissions by country sorted by highest emitters
SELECT 
    c.country_name,
    c.region,
    SUM(ge.total_emissions) AS total_emissions,
    AVG(ge.per_capita_emissions) AS avg_per_capita_emissions
FROM 
    GlobalEmission ge
JOIN 
    Country c ON ge.country_id = c.country_id
GROUP BY 
    c.country_id, c.country_name, c.region
ORDER BY 
    total_emissions DESC;

-- 3. Filtering with multiple conditions - Find active policies with significant reduction targets
SELECT 
    p.policy_id,
    p.policy_name,
    c.country_name,
    p.implementation_date,
    p.target_reduction_percentage,
    u.username AS created_by_user
FROM 
    SustainabilityPolicy p
JOIN 
    Country c ON p.country_id = c.country_id
JOIN 
    User u ON p.created_by = u.user_id
WHERE 
    p.status = 'Active'
    AND p.target_reduction_percentage > 20
ORDER BY 
    p.target_reduction_percentage DESC;

-- 4. Complex JOIN with multi-table relationships - Business energy profile analysis
SELECT 
    b.business_name,
    c.country_name,
    b.industry_type,
    es.source_name,
    es.is_renewable,
    bes.current_usage,
    bes.unit_cost,
    (bes.current_usage * es.carbon_factor) AS estimated_emissions,
    (bes.current_usage * bes.unit_cost) AS annual_energy_cost
FROM 
    Business b
JOIN 
    BusinessEnergySource bes ON b.business_id = bes.business_id
JOIN 
    EnergySource es ON bes.source_id = es.source_id
JOIN 
    Country c ON b.country_id = c.country_id
ORDER BY 
    b.business_name, es.is_renewable DESC;

-- 5. Subquery - Find businesses with above-average emissions reduction potential
SELECT 
    b.business_id,
    b.business_name,
    b.industry_type,
    bes.avg_emissions_reduction,
    bes.max_cost_savings
FROM 
    Business b
JOIN 
    BusinessEmissionStats bes ON b.business_id = bes.business_id
WHERE 
    bes.avg_emissions_reduction > (
        SELECT AVG(avg_emissions_reduction) 
        FROM BusinessEmissionStats
    )
ORDER BY 
    bes.avg_emissions_reduction DESC;

-- 6. HAVING clause - Countries with more than one active sustainability policy
SELECT 
    c.country_name,
    COUNT(p.policy_id) AS policy_count,
    AVG(p.target_reduction_percentage) AS avg_reduction_target
FROM 
    Country c
JOIN 
    SustainabilityPolicy p ON c.country_id = p.country_id
WHERE 
    p.status = 'Active'
GROUP BY 
    c.country_id, c.country_name
HAVING 
    COUNT(p.policy_id) > 1
ORDER BY 
    policy_count DESC;

-- 7. WITH clause (Common Table Expression) - Analysis of businesses with renewable energy sources
WITH RenewableUsage AS (
    SELECT 
        b.business_id,
        b.business_name,
        SUM(CASE WHEN es.is_renewable = TRUE THEN bes.current_usage ELSE 0 END) AS renewable_usage,
        SUM(bes.current_usage) AS total_usage
    FROM 
        Business b
    JOIN 
        BusinessEnergySource bes ON b.business_id = bes.business_id
    JOIN 
        EnergySource es ON bes.source_id = es.source_id
    GROUP BY 
        b.business_id, b.business_name
)
SELECT 
    ru.business_name,
    ru.renewable_usage,
    ru.total_usage,
    ROUND((ru.renewable_usage / ru.total_usage) * 100, 2) AS renewable_percentage,
    CASE 
        WHEN (ru.renewable_usage / ru.total_usage) >= 0.75 THEN 'Excellent'
        WHEN (ru.renewable_usage / ru.total_usage) >= 0.50 THEN 'Good'
        WHEN (ru.renewable_usage / ru.total_usage) >= 0.25 THEN 'Fair'
        ELSE 'Poor'
    END AS sustainability_rating
FROM 
    RenewableUsage ru
ORDER BY 
    renewable_percentage DESC;

-- 8. JOIN with GROUP BY and window functions - Ranking countries by emission reduction
SELECT 
    c.country_name,
    MAX(ge1.total_emissions) AS emissions_2020,
    MAX(ge2.total_emissions) AS emissions_2021,
    (MAX(ge2.total_emissions) - MAX(ge1.total_emissions)) AS emissions_change,
    ROUND(((MAX(ge2.total_emissions) - MAX(ge1.total_emissions)) / MAX(ge1.total_emissions)) * 100, 2) AS percent_change,
    RANK() OVER (ORDER BY (MAX(ge2.total_emissions) - MAX(ge1.total_emissions)) / MAX(ge1.total_emissions)) AS reduction_rank
FROM 
    Country c
JOIN 
    GlobalEmission ge1 ON c.country_id = ge1.country_id AND ge1.year = 2020
JOIN 
    GlobalEmission ge2 ON c.country_id = ge2.country_id AND ge2.year = 2021
GROUP BY 
    c.country_id, c.country_name
ORDER BY 
    percent_change;

-- 9. LEFT JOIN to find businesses without simulations
SELECT 
    b.business_id,
    b.business_name,
    b.industry_type,
    b.registration_date
FROM 
    Business b
LEFT JOIN 
    EmissionSimulation es ON b.business_id = es.business_id
WHERE 
    es.simulation_id IS NULL
ORDER BY 
    b.registration_date;

-- 10. Complex analysis - Finding the most cost-effective renewable energy transitions
SELECT 
    b.business_name,
    es_current.source_name AS current_source,
    es_new.source_name AS proposed_source,
    sd.energy_amount,
    sd.emissions_reduction,
    sd.implementation_cost,
    sd.annual_savings,
    ROUND(sd.annual_savings / sd.implementation_cost * 100, 2) AS roi_percentage,
    ROUND(sd.implementation_cost / sd.emissions_reduction, 2) AS cost_per_ton_reduction
FROM 
    SimulationDetail sd
JOIN 
    EmissionSimulation es ON sd.simulation_id = es.simulation_id
JOIN 
    Business b ON es.business_id = b.business_id
JOIN 
    EnergySource es_current ON sd.current_source_id = es_current.source_id
JOIN 
    EnergySource es_new ON sd.new_source_id = es_new.source_id
WHERE 
    es_new.is_renewable = TRUE
ORDER BY 
    roi_percentage DESC;

-- 11. UNION to combine data from multiple queries - All recent system activities
SELECT 
    'New User Registration' AS activity_type,
    CONCAT(u.first_name, ' ', u.last_name) AS name,
    u.created_at AS activity_date
FROM 
    User u
WHERE 
    u.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)

UNION

SELECT 
    'Business Registration' AS activity_type,
    b.business_name AS name,
    b.created_at AS activity_date
FROM 
    Business b
WHERE 
    b.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)

UNION

SELECT 
    'New Simulation' AS activity_type,
    es.simulation_name AS name,
    es.simulation_date AS activity_date
FROM 
    EmissionSimulation es
WHERE 
    es.simulation_date > DATE_SUB(NOW(), INTERVAL 30 DAY)

UNION

SELECT 
    'New Report' AS activity_type,
    ar.title AS name,
    ar.created_at AS activity_date
FROM 
    AnalysisReport ar
WHERE 
    ar.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY 
    activity_date DESC;

-- 12. Finding policy effectiveness through emissions data
SELECT 
    p.policy_name,
    c.country_name,
    p.implementation_date,
    ROUND(AVG(CASE WHEN ge.year < YEAR(p.implementation_date) THEN ge.total_emissions ELSE NULL END)) AS avg_emissions_before,
    ROUND(AVG(CASE WHEN ge.year >= YEAR(p.implementation_date) THEN ge.total_emissions ELSE NULL END)) AS avg_emissions_after,
    ROUND((AVG(CASE WHEN ge.year >= YEAR(p.implementation_date) THEN ge.total_emissions ELSE NULL END) - 
           AVG(CASE WHEN ge.year < YEAR(p.implementation_date) THEN ge.total_emissions ELSE NULL END)) /
          AVG(CASE WHEN ge.year < YEAR(p.implementation_date) THEN ge.total_emissions ELSE NULL END) * 100, 2) AS percent_change
FROM 
    SustainabilityPolicy p
JOIN 
    Country c ON p.country_id = c.country_id
JOIN 
    GlobalEmission ge ON c.country_id = ge.country_id
WHERE 
    p.implementation_date IS NOT NULL
    AND p.implementation_date < DATE_SUB(NOW(), INTERVAL 1 YEAR)
GROUP BY 
    p.policy_id, p.policy_name, c.country_name, p.implementation_date
ORDER BY 
    percent_change; 