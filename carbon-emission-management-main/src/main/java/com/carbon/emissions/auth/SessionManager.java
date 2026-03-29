package com.carbon.emissions.auth;

import java.sql.SQLException;

/**
 * Manages user sessions across the application
 */
public class SessionManager {
    
    private static UserAccount currentUser = null;
    private static SessionManager instance = null;
    
    // Private constructor for singleton pattern
    private SessionManager() {
    }
    
    /**
     * Get the singleton instance of SessionManager
     * 
     * @return The singleton SessionManager instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Set the current user for this session
     * 
     * @param user The UserAccount to set as current user
     */
    public void setCurrentUser(UserAccount user) {
        currentUser = user;
    }
    
    /**
     * Get the current user for this session
     * 
     * @return The current UserAccount
     * @throws IllegalStateException if no user is logged in
     */
    public UserAccount getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in");
        }
        return currentUser;
    }
    
    /**
     * Check if a user is currently logged in
     * 
     * @return true if a user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Log out the current user
     * 
     * @throws SQLException if there's an error recording the logout in the database
     */
    public void logout() throws SQLException {
        if (currentUser != null) {
            currentUser.recordLogout();
            currentUser = null;
        }
    }
    
    /**
     * Check if the current user has admin privileges
     * 
     * @return true if the current user is an admin, false otherwise
     */
    public boolean isAdmin() {
        return isLoggedIn() && "Admin".equals(currentUser.getRoleName());
    }
    
    /**
     * Check if the current user has analyst privileges
     * 
     * @return true if the current user is an analyst, false otherwise
     */
    public boolean isAnalyst() {
        return isLoggedIn() && "Analyst".equals(currentUser.getRoleName());
    }
    
    /**
     * Clear the current session
     */
    public void clearSession() {
        currentUser = null;
    }
} 