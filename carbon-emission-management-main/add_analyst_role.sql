USE carbon_emissions_db;

-- Check if the 'Analyst' role already exists
SET @analyst_exists = (SELECT COUNT(*) FROM Role WHERE role_name = 'Analyst');

-- If 'Analyst' role doesn't exist, add it
INSERT INTO Role (role_name, description, created_at, updated_at)
SELECT 'Analyst', 'Data analyst with access to analytical dashboards', NOW(), NOW()
WHERE @analyst_exists = 0;

-- Add a sample analyst account if it doesn't exist
SET @analyst_user_exists = (SELECT COUNT(*) FROM User WHERE username = 'analyst');

-- If the analyst user doesn't exist, add it
INSERT INTO User (role_id, username, password_hash, email, first_name, last_name, is_active, created_at, updated_at)
SELECT 
    (SELECT role_id FROM Role WHERE role_name = 'Analyst'),
    'analyst',
    -- Using the same password hash algorithm as in the application
    SHA2(CONCAT('password', 'carbon_emissions_salt'), 256),
    'analyst@example.com',
    'Data',
    'Analyst',
    1,
    NOW(),
    NOW()
WHERE @analyst_user_exists = 0;

-- Check for success
SELECT 'Analyst role and user created successfully' AS Result; 