package com.carbon.emissions.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import com.carbon.emissions.CarbonEmissionsDBConnection;

/**
 * Class to represent and manage user accounts
 */
public class UserAccount {
    private int userId;
    private int roleId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String roleName;
    private Date lastLogin;
    private boolean isActive;
    private int businessId = -1; // Default to -1 if no business is associated
    
    // Constructor for new user creation
    public UserAccount(String username, String email, String firstName, String lastName, String roleName) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roleName = roleName;
        this.isActive = true;
    }
    
    // Constructor for loading existing user
    public UserAccount(int userId, int roleId, String username, String email, 
                       String firstName, String lastName, Date lastLogin, boolean isActive) {
        this.userId = userId;
        this.roleId = roleId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.lastLogin = lastLogin;
        this.isActive = isActive;
        
        // Load the role name
        try {
            this.roleName = getRoleNameFromDB(roleId);
        } catch (SQLException e) {
            System.err.println("Error loading role name: " + e.getMessage());
            this.roleName = "Unknown";
        }
        
        // Try to find associated business ID
        try {
            this.businessId = getAssociatedBusinessId();
        } catch (SQLException e) {
            System.err.println("Error finding associated business: " + e.getMessage());
        }
    }
    
    /**
     * Authenticate a user with username and password
     * @param username The username
     * @param password The plain text password
     * @return UserAccount if authenticated, null otherwise
     */
    public static UserAccount authenticate(String username, String password) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            // Query to get user data by username
            String sql = "SELECT u.user_id, u.role_id, u.username, u.password_hash, u.email, " +
                        "u.first_name, u.last_name, u.last_login, u.is_active " +
                        "FROM User u WHERE u.username = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String passwordHash = hashPassword(password);
                
                if (passwordHash.equals(storedHash)) {
                    // Passwords match, create user account
                    UserAccount account = new UserAccount(
                        rs.getInt("user_id"),
                        rs.getInt("role_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getTimestamp("last_login"),
                        rs.getBoolean("is_active")
                    );
                    
                    // Update last login time
                    updateLastLogin(account.getUserId());
                    
                    // Record login history
                    recordLoginHistory(account.getUserId());
                    
                    return account;
                }
            }
            
            // Authentication failed
            return null;
            
        } finally {
            // Close resources in reverse order
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Register a new user account
     * @param username The username
     * @param password The plain text password (will be hashed)
     * @param email User's email
     * @param firstName User's first name
     * @param lastName User's last name
     * @param roleName User's role (Admin, Analyst, NormalUser)
     * @return The new UserAccount if registration successful, null otherwise
     */
    public static UserAccount register(String username, String password, String email, 
                                     String firstName, String lastName, String roleName) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            // Check if username or email already exists
            String checkSql = "SELECT COUNT(*) FROM User WHERE username = ? OR email = ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setString(1, username);
            stmt.setString(2, email);
            
            rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                // Username or email already exists
                return null;
            }
            
            // Get role ID
            int roleId = getRoleId(roleName);
            if (roleId == -1) {
                // Role not found
                return null;
            }
            
            // Hash the password
            String passwordHash = hashPassword(password);
            
            // Insert new user
            String insertSql = "INSERT INTO User (role_id, username, password_hash, email, " +
                            "first_name, last_name, is_active) " +
                            "VALUES (?, ?, ?, ?, ?, ?, TRUE)";
            
            if (stmt != null) stmt.close();
            
            stmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, roleId);
            stmt.setString(2, username);
            stmt.setString(3, passwordHash);
            stmt.setString(4, email);
            stmt.setString(5, firstName);
            stmt.setString(6, lastName);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                return null; // Insert failed
            }
            
            // Get the generated ID
            rs = stmt.getGeneratedKeys();
            int userId = -1;
            if (rs.next()) {
                userId = rs.getInt(1);
            } else {
                return null; // Failed to get ID
            }
            
            // Create and return the new user account
            return new UserAccount(userId, roleId, username, email, firstName, lastName, null, true);
            
        } finally {
            // Close resources in reverse order
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Get the role ID from the role name
     */
    public static int getRoleId(String roleName) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            String sql = "SELECT role_id FROM Role WHERE role_name = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, roleName);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("role_id");
            }
            
            return -1; // Role not found
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Update the last login time for a user
     */
    private static void updateLastLogin(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            String sql = "UPDATE User SET last_login = NOW() WHERE user_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Record login history
     */
    private static void recordLoginHistory(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            String sql = "INSERT INTO LoginHistory (user_id, login_time, ip_address) VALUES (?, NOW(), ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, "127.0.0.1"); // In a real application, get actual IP
            
            stmt.executeUpdate();
            
        } finally {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Record logout time
     */
    public void recordLogout() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            // Get the latest login ID for this user
            String loginIdSql = "SELECT login_id FROM LoginHistory " +
                             "WHERE user_id = ? AND logout_time IS NULL " +
                             "ORDER BY login_time DESC LIMIT 1";
            
            stmt = conn.prepareStatement(loginIdSql);
            stmt.setInt(1, this.userId);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                int loginId = rs.getInt("login_id");
                
                // Update the logout time
                String updateSql = "UPDATE LoginHistory SET logout_time = NOW() " +
                                "WHERE login_id = ? AND user_id = ?";
                
                if (stmt != null) stmt.close();
                
                stmt = conn.prepareStatement(updateSql);
                stmt.setInt(1, loginId);
                stmt.setInt(2, this.userId);
                
                stmt.executeUpdate();
            }
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Get the role name for a role ID
     */
    private String getRoleNameFromDB(int roleId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            String sql = "SELECT role_name FROM Role WHERE role_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, roleId);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("role_name");
            }
            
            return "Unknown";
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Get business ID associated with this user
     */
    private int getAssociatedBusinessId() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            // Query to find business created by this user
            String sql = "SELECT business_id FROM Business WHERE created_by = ? LIMIT 1";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, this.userId);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("business_id");
            }
            
            return -1; // No business found
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Hash a password using SHA-256
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    // Getters and setters
    
    public int getUserId() {
        return userId;
    }
    
    public int getRoleId() {
        return roleId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public Date getLastLogin() {
        return lastLogin;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public int getBusinessId() {
        return businessId;
    }
    
    public void setBusinessId(int businessId) {
        this.businessId = businessId;
    }
    
    /**
     * Create a new business associated with this user
     */
    public int createBusiness(String businessName, String industryType, 
                             String address, String city, String state, 
                             String postalCode, int countryId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            String sql = "INSERT INTO Business (business_name, industry_type, " +
                       "street_address, city, state_province, postal_code, " +
                       "country_id, registration_date, created_by) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, CURDATE(), ?)";
            
            stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, businessName);
            stmt.setString(2, industryType);
            stmt.setString(3, address);
            stmt.setString(4, city);
            stmt.setString(5, state);
            stmt.setString(6, postalCode);
            stmt.setInt(7, countryId);
            stmt.setInt(8, this.userId);
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                return -1; // Insert failed
            }
            
            // Get the generated ID
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int businessId = rs.getInt(1);
                this.businessId = businessId;
                return businessId;
            }
            
            return -1; // Failed to get ID
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
} 