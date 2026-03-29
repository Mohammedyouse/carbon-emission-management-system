package com.carbon.emissions;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carbon.emissions.auth.UserAccount;

/**
 * Data Access class for interacting with the carbon_emissions_db
 */
public class CarbonEmissionsDataAccess {
    
    /**
     * Calls the update_business_emission_stats stored procedure
     * 
     * @param businessId the business ID to update stats for
     * @return the message returned by the procedure
     * @throws SQLException if a database error occurs
     */
    public String updateBusinessEmissionStats(int businessId) throws SQLException {
        String result = null;
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL update_business_emission_stats(?, ?)}")) {
            
            stmt.setInt(1, businessId);
            stmt.setBoolean(2, true); // return message
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result = rs.getString("message");
                }
            }
        }
        
        return result;
    }
    
    /**
     * Gets simulation recommendations for a specific simulation
     * 
     * @param simulationId the simulation ID
     * @return the recommendation text and detailed breakdown
     * @throws SQLException if a database error occurs
     */
    public Map<String, Object> getSimulationRecommendations(int simulationId) throws SQLException {
        Map<String, Object> results = new HashMap<>();
        String recommendation = null;
        List<Map<String, Object>> details = new ArrayList<>();
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall("{CALL generate_simulation_recommendations(?)}")) {
            
            stmt.setInt(1, simulationId);
            
            boolean hasResults = stmt.execute();
            
            // First result set contains the recommendation
            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        recommendation = rs.getString("simulation_recommendation");
                    }
                }
            }
            
            // Next result set contains the detailed breakdown
            if (stmt.getMoreResults()) {
                try (ResultSet rs = stmt.getResultSet()) {
                    while (rs.next()) {
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("currentSource", rs.getString("current_source"));
                        detail.put("proposedSource", rs.getString("proposed_source"));
                        detail.put("energyAmount", rs.getDouble("energy_amount"));
                        detail.put("emissionsReduction", rs.getDouble("emissions_reduction"));
                        detail.put("reductionPerUnit", rs.getDouble("reduction_per_unit"));
                        detail.put("implementationCost", rs.getDouble("implementation_cost"));
                        detail.put("annualSavings", rs.getDouble("annual_savings"));
                        detail.put("roiPercentage", rs.getDouble("roi_percentage"));
                        details.add(detail);
                    }
                }
            }
        }
        
        results.put("recommendation", recommendation);
        results.put("details", details);
        
        return results;
    }
    
    /**
     * Calculates emissions using the database function
     * 
     * @param sourceId the energy source ID
     * @param energyAmount the amount of energy
     * @return the calculated emissions
     * @throws SQLException if a database error occurs
     */
    public double calculateEmissions(int sourceId, double energyAmount) throws SQLException {
        double emissions = 0;
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT calculate_emissions(?, ?) AS emissions")) {
            
            stmt.setInt(1, sourceId);
            stmt.setDouble(2, energyAmount);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    emissions = rs.getDouble("emissions");
                }
            }
        }
        
        return emissions;
    }
    
    /**
     * Gets a list of all users in the system
     *
     * @return List of user accounts
     * @throws SQLException if a database error occurs
     */
    public List<UserAccount> getAllUsers() throws SQLException {
        List<UserAccount> users = new ArrayList<>();
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT u.user_id, u.role_id, u.username, u.email, " +
                 "u.first_name, u.last_name, u.last_login, u.is_active " +
                 "FROM User u ORDER BY u.username")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserAccount user = new UserAccount(
                        rs.getInt("user_id"),
                        rs.getInt("role_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getTimestamp("last_login"),
                        rs.getBoolean("is_active")
                    );
                    users.add(user);
                }
            }
        }
        
        return users;
    }
    
    /**
     * Search for users by username, email, or name
     *
     * @param searchTerm The search term
     * @return List of matching user accounts
     * @throws SQLException if a database error occurs
     */
    public List<UserAccount> searchUsers(String searchTerm) throws SQLException {
        List<UserAccount> users = new ArrayList<>();
        String searchPattern = "%" + searchTerm + "%";
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT u.user_id, u.role_id, u.username, u.email, " +
                 "u.first_name, u.last_name, u.last_login, u.is_active " +
                 "FROM User u WHERE u.username LIKE ? OR u.email LIKE ? " +
                 "OR u.first_name LIKE ? OR u.last_name LIKE ? " +
                 "ORDER BY u.username")) {
            
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UserAccount user = new UserAccount(
                        rs.getInt("user_id"),
                        rs.getInt("role_id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getTimestamp("last_login"),
                        rs.getBoolean("is_active")
                    );
                    users.add(user);
                }
            }
        }
        
        return users;
    }
    
    /**
     * Delete a user by ID
     *
     * @param userId The ID of the user to delete
     * @return true if successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean deleteUser(int userId) throws SQLException {
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM User WHERE user_id = ?")) {
            
            stmt.setInt(1, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Update user information
     *
     * @param userId User ID to update
     * @param email Updated email
     * @param firstName Updated first name
     * @param lastName Updated last name
     * @param roleId Updated role ID
     * @param isActive Updated active status
     * @return true if successful, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean updateUser(int userId, String email, String firstName, 
                             String lastName, int roleId, boolean isActive) throws SQLException {
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE User SET email = ?, first_name = ?, last_name = ?, " +
                 "role_id = ?, is_active = ? WHERE user_id = ?")) {
            
            stmt.setString(1, email);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setInt(4, roleId);
            stmt.setBoolean(5, isActive);
            stmt.setInt(6, userId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
    
    /**
     * Get all available roles
     *
     * @return List of role names and IDs
     * @throws SQLException if a database error occurs
     */
    public List<Map<String, Object>> getAllRoles() throws SQLException {
        List<Map<String, Object>> roles = new ArrayList<>();
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT role_id, role_name, description FROM Role")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> role = new HashMap<>();
                    role.put("roleId", rs.getInt("role_id"));
                    role.put("roleName", rs.getString("role_name"));
                    role.put("description", rs.getString("description"));
                    roles.add(role);
                }
            }
        }
        
        return roles;
    }
    
    /**
     * Get user statistics for admin dashboard
     *
     * @return Map containing user statistics
     * @throws SQLException if a database error occurs
     */
    public Map<String, Object> getUserStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection()) {
            // Get total active users
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as active_users FROM User WHERE is_active = TRUE")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("activeUsers", rs.getInt("active_users"));
                    }
                }
            }
            
            // Get new users in the last 30 days
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COUNT(*) as new_users FROM User WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.put("newUsers", rs.getInt("new_users"));
                    }
                }
            }
            
            // Get user count by role
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT r.role_name, COUNT(*) as count FROM User u " +
                    "JOIN Role r ON u.role_id = r.role_id " +
                    "GROUP BY r.role_name")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    Map<String, Integer> roleStats = new HashMap<>();
                    while (rs.next()) {
                        roleStats.put(rs.getString("role_name"), rs.getInt("count"));
                    }
                    stats.put("usersByRole", roleStats);
                }
            }
        }
        
        return stats;
    }
    
    /**
     * Example usage of the data access methods
     */
    public static void main(String[] args) {
        CarbonEmissionsDataAccess dao = new CarbonEmissionsDataAccess();
        
        try {
            // Example: Update business stats
            String updateResult = dao.updateBusinessEmissionStats(1);
            System.out.println("Update result: " + updateResult);
            
            // Example: Get simulation recommendations
            Map<String, Object> recommendations = dao.getSimulationRecommendations(1);
            System.out.println("Recommendation: " + recommendations.get("recommendation"));
            
            // Example: Calculate emissions
            double emissions = dao.calculateEmissions(1, 1000000.0);
            System.out.println("Calculated emissions: " + emissions);
            
            // Example: Get all users
            List<UserAccount> users = dao.getAllUsers();
            System.out.println("Total users: " + users.size());
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 