-- Carbon Emission Analysis & Business Carbon Savings Simulator Database Schema

-- Create database

DROP DATABASE IF EXISTS carbon_emissions_db;
CREATE DATABASE carbon_emissions_db;
USE carbon_emissions_db;

-- Role table for user roles
CREATE TABLE Role (
    role_id INT PRIMARY KEY AUTO_INCREMENT,
    role_name ENUM('Admin', 'Analyst', 'NormalUser') NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- User table with IS-A relationship (inheritance) implemented through role_id
CREATE TABLE User (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    first_name VARCHAR(50) NOT NULL, -- Part of composite attribute (name)
    last_name VARCHAR(50) NOT NULL,  -- Part of composite attribute (name)
    is_active BOOLEAN DEFAULT TRUE,
    last_login DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES Role(role_id)
);

-- LoginHistory (weak entity dependent on User)
CREATE TABLE LoginHistory (
    login_id INT AUTO_INCREMENT,
    user_id INT NOT NULL,
    login_time DATETIME NOT NULL,
    logout_time DATETIME,
    ip_address VARCHAR(45),
    device_info VARCHAR(255),
    PRIMARY KEY (login_id, user_id), -- Composite primary key for weak entity
    FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE
);

-- Countries table
CREATE TABLE Country (
    country_id INT PRIMARY KEY AUTO_INCREMENT,
    country_name VARCHAR(100) UNIQUE NOT NULL,
    country_code CHAR(2) UNIQUE NOT NULL,
    region VARCHAR(50),
    population BIGINT, -- For derived per capita calculations
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- EnergySource table for different types of energy sources
CREATE TABLE EnergySource (
    source_id INT PRIMARY KEY AUTO_INCREMENT,
    source_name VARCHAR(50) UNIQUE NOT NULL,
    is_renewable BOOLEAN NOT NULL,
    carbon_factor DECIMAL(10,4), -- CO2 emissions per unit of energy
    unit_of_measure VARCHAR(20) NOT NULL, -- kWh, etc.
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- GlobalEmission table for storing historical emissions data
CREATE TABLE GlobalEmission (
    emission_id INT PRIMARY KEY AUTO_INCREMENT,
    country_id INT NOT NULL,
    year INT NOT NULL,
    total_emissions DECIMAL(20,2) NOT NULL, -- in metric tons CO2
    per_capita_emissions DECIMAL(10,4), -- derived attribute
    gdp_millions_usd DECIMAL(20,2),
    emissions_per_gdp DECIMAL(10,4), -- derived attribute
    data_source VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES Country(country_id),
    UNIQUE KEY (country_id, year) -- Each country can have only one emission record per year
);

-- SustainabilityPolicy table for tracking policies and initiatives
CREATE TABLE SustainabilityPolicy (
    policy_id INT PRIMARY KEY AUTO_INCREMENT,
    country_id INT NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    description TEXT, -- Complex attribute
    implementation_date DATE,
    expiration_date DATE,
    target_reduction_percentage DECIMAL(5,2),
    status ENUM('Proposed', 'Active', 'Expired', 'Cancelled') NOT NULL,
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES Country(country_id),
    FOREIGN KEY (created_by) REFERENCES User(user_id)
);

-- Business table for company information
CREATE TABLE Business (
    business_id INT PRIMARY KEY AUTO_INCREMENT,
    business_name VARCHAR(100) NOT NULL,
    industry_type VARCHAR(50),
    street_address VARCHAR(100), -- Part of composite attribute (location)
    city VARCHAR(50),           -- Part of composite attribute (location)
    state_province VARCHAR(50),  -- Part of composite attribute (location)
    postal_code VARCHAR(20),     -- Part of composite attribute (location)
    country_id INT NOT NULL,
    annual_revenue_usd DECIMAL(20,2),
    employee_count INT,
    registration_date DATE NOT NULL,
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES Country(country_id),
    FOREIGN KEY (created_by) REFERENCES User(user_id)
);

-- BusinessEnergySources (Multi-valued attribute relationship)
CREATE TABLE BusinessEnergySource (
    business_id INT NOT NULL,
    source_id INT NOT NULL,
    current_usage DECIMAL(20,2), -- Current energy consumption
    unit_cost DECIMAL(10,4),    -- Cost per unit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (business_id, source_id),
    FOREIGN KEY (business_id) REFERENCES Business(business_id) ON DELETE CASCADE,
    FOREIGN KEY (source_id) REFERENCES EnergySource(source_id)
);

-- EmissionSimulation for business simulation scenarios
CREATE TABLE EmissionSimulation (
    simulation_id INT PRIMARY KEY AUTO_INCREMENT,
    business_id INT NOT NULL,
    user_id INT NOT NULL,
    simulation_name VARCHAR(100) NOT NULL,
    simulation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    current_total_emissions DECIMAL(20,2), -- Current emissions before changes
    target_total_emissions DECIMAL(20,2),  -- Target emissions after changes
    estimated_cost_savings DECIMAL(20,2),  -- Derived attribute
    payback_period_months INT,             -- Derived attribute
    notes TEXT,
    FOREIGN KEY (business_id) REFERENCES Business(business_id),
    FOREIGN KEY (user_id) REFERENCES User(user_id)
);

-- SimulationDetail for storing energy transition details in simulations
CREATE TABLE SimulationDetail (
    detail_id INT PRIMARY KEY AUTO_INCREMENT,
    simulation_id INT NOT NULL,
    current_source_id INT NOT NULL,
    new_source_id INT NOT NULL,
    energy_amount DECIMAL(20,2) NOT NULL,
    emissions_reduction DECIMAL(20,2), -- Derived attribute
    implementation_cost DECIMAL(20,2),
    annual_savings DECIMAL(20,2),      -- Derived attribute
    incentive_amount DECIMAL(20,2),
    FOREIGN KEY (simulation_id) REFERENCES EmissionSimulation(simulation_id) ON DELETE CASCADE,
    FOREIGN KEY (current_source_id) REFERENCES EnergySource(source_id),
    FOREIGN KEY (new_source_id) REFERENCES EnergySource(source_id)
);

-- BusinessEmissionStats (Aggregation entity)
CREATE TABLE BusinessEmissionStats (
    stats_id INT PRIMARY KEY AUTO_INCREMENT,
    business_id INT UNIQUE NOT NULL,
    total_simulations INT DEFAULT 0,
    avg_emissions_reduction DECIMAL(20,2),
    max_cost_savings DECIMAL(20,2),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES Business(business_id) ON DELETE CASCADE
);

-- Incentive table for government incentives for renewable energy adoption
CREATE TABLE Incentive (
    incentive_id INT PRIMARY KEY AUTO_INCREMENT,
    country_id INT NOT NULL,
    incentive_name VARCHAR(100) NOT NULL,
    description TEXT,
    applicable_sources TEXT, -- Comma-separated list of applicable energy source IDs
    incentive_type ENUM('Tax Credit', 'Rebate', 'Grant', 'Loan', 'Other') NOT NULL,
    amount_percentage DECIMAL(5,2), -- Percentage of implementation cost
    max_amount DECIMAL(20,2),      -- Maximum incentive amount
    start_date DATE NOT NULL,
    end_date DATE,
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES Country(country_id),
    FOREIGN KEY (created_by) REFERENCES User(user_id)
);

-- Analysis Report table
CREATE TABLE AnalysisReport (
    report_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    report_type ENUM('Global Trend', 'Country Analysis', 'Business Simulation', 'Policy Impact') NOT NULL,
    created_by INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    report_data TEXT, -- JSON formatted data for visualization
    is_published BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (created_by) REFERENCES User(user_id)
);

-- Create views for different user roles dashboard
CREATE VIEW AdminDashboard AS
SELECT 
    u.user_id, 
    CONCAT(u.first_name, ' ', u.last_name) AS full_name,
    COUNT(DISTINCT b.business_id) AS total_businesses,
    COUNT(DISTINCT e.emission_id) AS total_emission_records,
    COUNT(DISTINCT s.simulation_id) AS total_simulations,
    COUNT(DISTINCT a.report_id) AS total_reports,
    COUNT(DISTINCT CASE WHEN u2.is_active = 1 THEN u2.user_id END) AS active_users,
    COUNT(DISTINCT CASE WHEN u2.is_active = 0 THEN u2.user_id END) AS inactive_users
FROM 
    User u
LEFT JOIN Business b ON b.created_by = u.user_id
LEFT JOIN GlobalEmission e ON 1=1
LEFT JOIN EmissionSimulation s ON s.user_id = u.user_id
LEFT JOIN AnalysisReport a ON a.created_by = u.user_id
LEFT JOIN User u2 ON 1=1
WHERE 
    u.role_id = (SELECT role_id FROM Role WHERE role_name = 'Admin')
GROUP BY 
    u.user_id, full_name;

CREATE VIEW AnalystDashboard AS
SELECT 
    u.user_id, 
    CONCAT(u.first_name, ' ', u.last_name) AS full_name,
    COUNT(DISTINCT s.simulation_id) AS simulations_created,
    COUNT(DISTINCT a.report_id) AS reports_created,
    COUNT(DISTINCT CASE WHEN a.is_published = 1 THEN a.report_id END) AS published_reports,
    COUNT(DISTINCT c.country_id) AS countries_analyzed,
    AVG(s.estimated_cost_savings) AS avg_cost_savings
FROM 
    User u
LEFT JOIN EmissionSimulation s ON s.user_id = u.user_id
LEFT JOIN AnalysisReport a ON a.created_by = u.user_id
LEFT JOIN GlobalEmission ge ON 1=1
LEFT JOIN Country c ON ge.country_id = c.country_id
WHERE 
    u.role_id = (SELECT role_id FROM Role WHERE role_name = 'Analyst')
GROUP BY 
    u.user_id, full_name;

CREATE VIEW NormalUserDashboard AS
SELECT 
    u.user_id, 
    CONCAT(u.first_name, ' ', u.last_name) AS full_name,
    COUNT(DISTINCT s.simulation_id) AS simulations_run,
    MAX(s.simulation_date) AS last_simulation_date,
    SUM(s.estimated_cost_savings) AS total_potential_savings,
    COUNT(DISTINCT b.business_id) AS businesses_registered
FROM 
    User u
LEFT JOIN EmissionSimulation s ON s.user_id = u.user_id
LEFT JOIN Business b ON b.created_by = u.user_id
WHERE 
    u.role_id = (SELECT role_id FROM Role WHERE role_name = 'NormalUser')
GROUP BY 
    u.user_id, full_name; 