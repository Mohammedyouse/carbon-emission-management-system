USE carbon_emissions_db;

-- Insert sample roles
INSERT INTO Role (role_name, description) VALUES
('Admin', 'System administrator with full access'),
('Analyst', 'Data analyst with reporting capabilities'),
('NormalUser', 'Regular user with simulation capabilities');

-- Insert sample users
INSERT INTO User (role_id, username, password_hash, email, first_name, last_name, is_active) VALUES
(1, 'admin1', SHA2('admin123', 256), 'admin@carbonsim.com', 'John', 'Admin', TRUE),
(2, 'analyst1', SHA2('analyst123', 256), 'analyst1@carbonsim.com', 'Sarah', 'Jones', TRUE),
(2, 'analyst2', SHA2('analyst456', 256), 'analyst2@carbonsim.com', 'Michael', 'Chen', TRUE),
(3, 'user1', SHA2('user123', 256), 'user1@example.com', 'Emily', 'Davis', TRUE),
(3, 'user2', SHA2('user456', 256), 'user2@example.com', 'Robert', 'Wilson', TRUE),
(3, 'user3', SHA2('user789', 256), 'user3@example.com', 'Jessica', 'Brown', TRUE),
(3, 'user4', SHA2('userpwd', 256), 'user4@example.com', 'David', 'Taylor', TRUE),
(3, 'user5', SHA2('userpass', 256), 'user5@example.com', 'Lisa', 'Miller', FALSE),
(2, 'analyst3', SHA2('analyst789', 256), 'analyst3@carbonsim.com', 'Alex', 'Johnson', TRUE),
(1, 'admin2', SHA2('admin456', 256), 'admin2@carbonsim.com', 'Patricia', 'Smith', TRUE);

-- Insert sample login history (weak entity)
INSERT INTO LoginHistory (user_id, login_time, logout_time, ip_address, device_info) VALUES
(1, '2023-01-01 08:30:00', '2023-01-01 17:45:00', '192.168.1.100', 'Chrome/Windows'),
(1, '2023-01-02 09:15:00', '2023-01-02 18:20:00', '192.168.1.100', 'Chrome/Windows'),
(2, '2023-01-02 08:00:00', '2023-01-02 16:30:00', '192.168.1.101', 'Firefox/MacOS'),
(3, '2023-01-03 10:00:00', '2023-01-03 15:45:00', '192.168.1.102', 'Safari/iOS'),
(4, '2023-01-03 09:30:00', '2023-01-03 17:00:00', '192.168.1.103', 'Edge/Windows'),
(5, '2023-01-04 08:45:00', '2023-01-04 16:15:00', '192.168.1.104', 'Chrome/Android'),
(2, '2023-01-05 08:30:00', '2023-01-05 17:30:00', '192.168.1.101', 'Firefox/MacOS'),
(6, '2023-01-05 09:00:00', '2023-01-05 18:00:00', '192.168.1.105', 'Chrome/Windows'),
(7, '2023-01-06 10:15:00', '2023-01-06 15:30:00', '192.168.1.106', 'Safari/MacOS'),
(8, '2023-01-07 09:45:00', '2023-01-07 17:15:00', '192.168.1.107', 'Firefox/Windows');

-- Insert sample countries
INSERT INTO Country (country_name, country_code, region, population) VALUES
('United States', 'US', 'North America', 331000000),
('China', 'CN', 'Asia', 1400000000),
('India', 'IN', 'Asia', 1380000000),
('Germany', 'DE', 'Europe', 83000000),
('Brazil', 'BR', 'South America', 212000000),
('United Kingdom', 'GB', 'Europe', 67000000),
('Canada', 'CA', 'North America', 38000000),
('Australia', 'AU', 'Oceania', 25000000),
('Japan', 'JP', 'Asia', 126000000),
('South Africa', 'ZA', 'Africa', 59000000);

-- Insert sample energy sources
INSERT INTO EnergySource (source_name, is_renewable, carbon_factor, unit_of_measure, description) VALUES
('Coal', FALSE, 0.9416, 'kWh', 'Traditional coal power generation'),
('Natural Gas', FALSE, 0.4108, 'kWh', 'Natural gas power generation'),
('Solar PV', TRUE, 0.0450, 'kWh', 'Photovoltaic solar panels'),
('Wind', TRUE, 0.0110, 'kWh', 'Wind turbines for electricity generation'),
('Hydroelectric', TRUE, 0.0240, 'kWh', 'Hydroelectric power generation'),
('Nuclear', FALSE, 0.0120, 'kWh', 'Nuclear power generation'),
('Biomass', TRUE, 0.2300, 'kWh', 'Organic material used as fuel'),
('Geothermal', TRUE, 0.0380, 'kWh', 'Heat energy derived from the earth'),
('Oil', FALSE, 0.6500, 'kWh', 'Petroleum-based power generation'),
('Tidal', TRUE, 0.0170, 'kWh', 'Energy from ocean tides');

-- Insert sample global emissions data
INSERT INTO GlobalEmission (country_id, year, total_emissions, per_capita_emissions, gdp_millions_usd, emissions_per_gdp, data_source) VALUES
(1, 2020, 4712000000.00, 14.24, 20940000.00, 0.2250, 'IEA Global Emissions Report'),
(1, 2021, 4890000000.00, 14.77, 22996000.00, 0.2127, 'IEA Global Emissions Report'),
(2, 2020, 10065000000.00, 7.18, 14723000.00, 0.6836, 'UN Climate Data Repository'),
(2, 2021, 11472000000.00, 8.19, 17734000.00, 0.6470, 'UN Climate Data Repository'),
(3, 2020, 2442000000.00, 1.77, 2622984.00, 0.9310, 'Indian Ministry of Environment'),
(3, 2021, 2709000000.00, 1.96, 3176295.00, 0.8530, 'Indian Ministry of Environment'),
(4, 2020, 644000000.00, 7.76, 3806000.00, 0.1692, 'European Environment Agency'),
(4, 2021, 675000000.00, 8.13, 4230000.00, 0.1596, 'European Environment Agency'),
(5, 2020, 417000000.00, 1.97, 1448000.00, 0.2880, 'Brazil Environmental Registry'),
(5, 2021, 428000000.00, 2.02, 1608000.00, 0.2662, 'Brazil Environmental Registry');

-- Insert sample sustainability policies
INSERT INTO SustainabilityPolicy (country_id, policy_name, description, implementation_date, expiration_date, target_reduction_percentage, status, created_by) VALUES
(1, 'Clean Power Plan', 'Federal regulation to reduce carbon emissions from power plants by shifting away from coal toward natural gas and renewables.', '2015-08-03', '2025-12-31', 32.00, 'Active', 1),
(2, 'China Carbon Trading Scheme', 'National emissions trading system covering power sector and heavy industries.', '2021-02-01', NULL, 18.00, 'Active', 2),
(3, 'National Solar Mission', 'Initiative to promote solar power development with target of 100 GW by 2022.', '2010-01-01', '2022-12-31', 12.50, 'Expired', 2),
(4, 'German Climate Action Plan', 'Strategy for achieving climate neutrality by 2050 with sector-specific targets.', '2016-11-14', '2050-12-31', 55.00, 'Active', 1),
(5, 'Amazon Deforestation Prevention', 'Multi-agency effort to reduce illegal deforestation in the Amazon region.', '2018-01-01', '2030-12-31', 15.00, 'Active', 3),
(6, 'UK Net Zero Strategy', 'Plan to decarbonize all sectors of the UK economy to meet net zero by 2050.', '2021-10-19', '2050-12-31', 78.00, 'Active', 1),
(7, 'Carbon Tax Framework', 'Implementation of carbon pricing with gradual increase over time.', '2019-04-01', NULL, 30.00, 'Active', 2),
(8, 'Renewable Energy Target', 'Commitment to reach 50% renewable energy in electricity generation by 2030.', '2020-01-01', '2030-12-31', 26.00, 'Active', 3),
(9, 'Green Growth Strategy', 'Comprehensive plan to achieve carbon neutrality by 2050 and foster green technologies.', '2020-12-25', '2050-12-31', 46.00, 'Active', 2),
(10, 'Renewable Energy Independent Power Producer Program', 'Initiative to procure renewable energy from independent producers.', '2011-08-03', '2025-12-31', 17.50, 'Active', 1);

-- Insert sample businesses
INSERT INTO Business (business_name, industry_type, street_address, city, state_province, postal_code, country_id, annual_revenue_usd, employee_count, registration_date, created_by) VALUES
('EcoTech Solutions', 'Manufacturing', '123 Green St', 'San Francisco', 'California', '94110', 1, 45000000.00, 210, '2010-06-15', 4),
('Global Energy Partners', 'Energy', '456 Power Ave', 'Houston', 'Texas', '77002', 1, 120000000.00, 850, '2005-03-22', 4),
('Sustainable Futures Ltd', 'Consulting', '789 Climate Rd', 'London', 'England', 'EC1V 9BX', 6, 12000000.00, 85, '2015-11-10', 5),
('GreenBuild Construction', 'Construction', '101 Eco Blvd', 'Toronto', 'Ontario', 'M5V 2H1', 7, 67000000.00, 320, '2012-04-05', 5),
('CleanWater Technologies', 'Utilities', '246 Aqua Lane', 'Sydney', 'New South Wales', '2000', 8, 89000000.00, 410, '2008-09-30', 6),
('Renewable Power Corp', 'Energy', '369 Sun Dr', 'Berlin', 'Berlin', '10115', 4, 156000000.00, 920, '2007-07-18', 6),
('EcoFashion Designs', 'Retail', '159 Sustainable Ave', 'Paris', 'Île-de-France', '75001', 6, 34000000.00, 175, '2014-02-28', 7),
('Green Transport Systems', 'Transportation', '753 Electric Rd', 'Tokyo', 'Tokyo', '100-0001', 9, 78000000.00, 380, '2011-10-10', 7),
('BioAgri Innovations', 'Agriculture', '852 Organic Way', 'São Paulo', 'São Paulo', '01310-100', 5, 23000000.00, 130, '2016-05-22', 8),
('CircularTech Industries', 'Technology', '741 Recycle St', 'Mumbai', 'Maharashtra', '400001', 3, 56000000.00, 260, '2013-12-03', 8);

-- Insert sample business energy sources (multi-valued attribute)
INSERT INTO BusinessEnergySource (business_id, source_id, current_usage, unit_cost) VALUES
(1, 1, 1500000.00, 0.08),  -- EcoTech using Coal
(1, 3, 500000.00, 0.12),   -- EcoTech using Solar
(2, 2, 4000000.00, 0.06),  -- Global Energy using Natural Gas
(2, 9, 2000000.00, 0.07),  -- Global Energy using Oil
(3, 2, 800000.00, 0.09),   -- Sustainable Futures using Natural Gas
(4, 1, 1200000.00, 0.07),  -- GreenBuild using Coal
(4, 7, 400000.00, 0.10),   -- GreenBuild using Biomass
(5, 5, 2500000.00, 0.05),  -- CleanWater using Hydroelectric
(6, 3, 3000000.00, 0.11),  -- Renewable Power using Solar
(6, 4, 2800000.00, 0.09),  -- Renewable Power using Wind
(7, 2, 900000.00, 0.08),   -- EcoFashion using Natural Gas
(8, 9, 1800000.00, 0.07),  -- Green Transport using Oil
(8, 4, 600000.00, 0.10),   -- Green Transport using Wind
(9, 7, 1100000.00, 0.06),  -- BioAgri using Biomass
(10, 1, 1700000.00, 0.07); -- CircularTech using Coal

-- Insert sample emission simulations
INSERT INTO EmissionSimulation (business_id, user_id, simulation_name, current_total_emissions, target_total_emissions, estimated_cost_savings, payback_period_months, notes) VALUES
(1, 4, 'Solar Expansion 2023', 1452000.00, 1070000.00, 138000.00, 36, 'Expanding solar capacity to reduce coal dependence'),
(2, 4, 'Wind Integration Plan', 1804000.00, 1402000.00, 215000.00, 42, 'Adding wind power to supplement natural gas'),
(3, 5, 'Full Renewable Transition', 328400.00, 121000.00, 92000.00, 60, 'Complete transition to renewable energy sources'),
(4, 5, 'Biomass Optimization', 1153200.00, 890000.00, 87000.00, 24, 'Increasing biomass usage and reducing coal'),
(5, 6, 'Solar+Wind Hybrid Model', 60000.00, 42000.00, 29000.00, 48, 'Adding solar and wind to complement hydroelectric'),
(6, 6, 'Geothermal Addition Project', 201000.00, 155000.00, 67000.00, 30, 'Adding geothermal to diversify renewable mix'),
(7, 7, 'Natural Gas to Solar Shift', 369720.00, 250000.00, 40000.00, 54, 'Phase out natural gas with solar implementation'),
(8, 7, 'Electric Fleet Conversion', 1191000.00, 875000.00, 108000.00, 48, 'Converting vehicle fleet to electric power'),
(9, 8, 'Integrated Renewable Plan', 253000.00, 186000.00, 32000.00, 36, 'Multi-source renewable energy integration'),
(10, 8, 'Coal to Natural Gas Shift', 1600520.00, 1200000.00, 70000.00, 18, 'Initial step to reduce emissions by switching to natural gas');

-- Insert sample simulation details
INSERT INTO SimulationDetail (simulation_id, current_source_id, new_source_id, energy_amount, emissions_reduction, implementation_cost, annual_savings, incentive_amount) VALUES
(1, 1, 3, 500000.00, 382000.00, 600000.00, 200000.00, 150000.00),
(2, 2, 4, 1000000.00, 402000.00, 900000.00, 250000.00, 180000.00),
(3, 2, 3, 800000.00, 207400.00, 750000.00, 120000.00, 100000.00),
(4, 1, 7, 400000.00, 263200.00, 350000.00, 120000.00, 80000.00),
(5, 5, 3, 500000.00, 18000.00, 400000.00, 40000.00, 60000.00),
(6, 3, 8, 300000.00, 46000.00, 500000.00, 100000.00, 125000.00),
(7, 2, 3, 600000.00, 119720.00, 550000.00, 80000.00, 90000.00),
(8, 9, 4, 800000.00, 316000.00, 750000.00, 150000.00, 120000.00),
(9, 7, 4, 400000.00, 67000.00, 300000.00, 50000.00, 75000.00),
(10, 1, 2, 1000000.00, 400520.00, 250000.00, 100000.00, 60000.00);

-- Insert sample business emission stats (aggregation entity)
INSERT INTO BusinessEmissionStats (business_id, total_simulations, avg_emissions_reduction, max_cost_savings, last_updated) VALUES
(1, 2, 382000.00, 138000.00, NOW()),
(2, 1, 402000.00, 215000.00, NOW()),
(3, 1, 207400.00, 92000.00, NOW()),
(4, 1, 263200.00, 87000.00, NOW()),
(5, 1, 18000.00, 29000.00, NOW()),
(6, 1, 46000.00, 67000.00, NOW()),
(7, 1, 119720.00, 40000.00, NOW()),
(8, 1, 316000.00, 108000.00, NOW()),
(9, 1, 67000.00, 32000.00, NOW()),
(10, 1, 400520.00, 70000.00, NOW());

-- Insert sample incentives
INSERT INTO Incentive (country_id, incentive_name, description, applicable_sources, incentive_type, amount_percentage, max_amount, start_date, end_date, created_by) VALUES
(1, 'Federal Solar Investment Tax Credit', 'Tax credit for solar energy systems', '3', 'Tax Credit', 26.00, 1000000.00, '2022-01-01', '2032-12-31', 1),
(1, 'Wind Energy Production Tax Credit', 'Tax credit based on electricity production from wind', '4', 'Tax Credit', 1.50, 500000.00, '2020-01-01', '2025-12-31', 1),
(2, 'Renewable Energy Subsidy Program', 'Subsidies for renewable energy installation', '3,4,5,7,8,10', 'Grant', 30.00, 2000000.00, '2021-03-01', '2026-02-28', 2),
(3, 'Solar Rooftop Promotion Scheme', 'Incentives for rooftop solar installation', '3', 'Rebate', 40.00, 100000.00, '2021-04-01', '2024-03-31', 2),
(4, 'Green Energy Transition Fund', 'Low-interest loans for transitioning to green energy', '3,4,5,7,8,10', 'Loan', 2.00, 5000000.00, '2020-06-01', NULL, 1),
(6, 'Smart Export Guarantee', 'Payment for renewable electricity exported to the grid', '3,4,5,7', 'Other', 15.00, 250000.00, '2020-01-01', NULL, 3),
(7, 'Clean Energy Fund', 'Grants for clean energy projects', '3,4,5,8,10', 'Grant', 35.00, 750000.00, '2019-07-01', '2027-06-30', 3),
(8, 'Renewable Energy Target Certificates', 'Tradable certificates for renewable energy generation', '3,4,5,7,8,10', 'Other', 20.00, 400000.00, '2021-01-01', '2030-12-31', 2),
(9, 'Zero-Emission Vehicle Subsidy', 'Subsidies for electric and hydrogen vehicles', '4', 'Rebate', 25.00, 10000.00, '2022-04-01', '2027-03-31', 1),
(10, 'Industrial Energy Efficiency Program', 'Support for energy efficiency measures in industry', '3,4,5,8', 'Grant', 45.00, 300000.00, '2021-10-01', '2026-09-30', 3);

-- Insert sample analysis reports
INSERT INTO AnalysisReport (title, description, report_type, created_by, report_data, is_published) VALUES
('Global Emission Trends 2015-2022', 'Analysis of emission trends across major economies', 'Global Trend', 2, '{"chart_type": "line", "data_points": 8, "countries": [1,2,3,4,5]}', TRUE),
('US Renewable Transition Impact', 'Impact assessment of renewable energy policies in the US', 'Policy Impact', 2, '{"chart_type": "bar", "data_points": 5, "policies": [1]}', TRUE),
('Business Case: EcoTech Renewable Integration', 'Cost-benefit analysis of renewable energy transition', 'Business Simulation', 3, '{"chart_type": "pie", "data_points": 3, "business_id": 1}', TRUE),
('China vs US Emission Comparison', 'Comparative analysis of emissions and reduction efforts', 'Country Analysis', 3, '{"chart_type": "comparison", "data_points": 10, "countries": [1,2]}', FALSE),
('European Climate Policies Effectiveness', 'Evaluation of climate policy impact across European nations', 'Policy Impact', 2, '{"chart_type": "heatmap", "data_points": 12, "countries": [4,6]}', TRUE),
('Manufacturing Sector Carbon Reduction Potential', 'Analysis of carbon reduction opportunities in manufacturing', 'Business Simulation', 3, '{"chart_type": "bubble", "data_points": 8, "business_ids": [1,10]}', FALSE),
('Renewable Energy ROI Analysis', 'Return on investment analysis for different renewable sources', 'Business Simulation', 9, '{"chart_type": "radar", "data_points": 6, "energy_sources": [3,4,5,7,8,10]}', TRUE),
('Transportation Sector Emission Forecasts', 'Predictive analysis of emissions in transportation sector', 'Global Trend', 9, '{"chart_type": "line", "data_points": 5, "forecast_years": 10}', FALSE),
('Incentive Program Uptake and Impact', 'Analysis of renewable energy incentive program adoption', 'Policy Impact', 3, '{"chart_type": "bar", "data_points": 10, "incentives": [1,2,3,4,5]}', TRUE),
('Emerging Markets Clean Energy Transition', 'Analysis of clean energy adoption in developing economies', 'Country Analysis', 2, '{"chart_type": "scatter", "data_points": 15, "countries": [3,5,10]}', TRUE); 