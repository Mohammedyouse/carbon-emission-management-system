package com.carbon.emissions.ui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.carbon.emissions.CarbonEmissionsDataAccess;
import com.carbon.emissions.auth.SessionManager;
import com.carbon.emissions.auth.UserAccount;

/**
 * Dashboard for administrators to manage users and system settings
 */
public class AdminDashboard extends JFrame {
    
    private String username;
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    
    // Data access components
    private CarbonEmissionsDataAccess dataAccess;
    private DefaultTableModel userTableModel;
    private JTable userTable;
    private JTextField searchField;
    private UserAccount currentUser;
    
    // Format for displaying dates
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    public AdminDashboard(String username) {
        this.username = username;
        
        // Initialize data access
        this.dataAccess = new CarbonEmissionsDataAccess();
        
        // Try to get the current user
        try {
            this.currentUser = SessionManager.getInstance().getCurrentUser();
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
        }
        
        // Set up the frame
        setTitle("Admin Dashboard - Carbon Emissions Management");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Create main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 250, 255));
        
        // Create header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create tabbed pane for different sections
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        // Add tabs
        tabbedPane.addTab("Dashboard", createDashboardPanel());
        tabbedPane.addTab("User Management", createUserManagementPanel());
        tabbedPane.addTab("System Settings", createSystemSettingsPanel());
        tabbedPane.addTab("Database Management", createDatabasePanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Load initial data
        refreshUserTable();
        refreshDashboardStats();
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(39, 174, 96)); // Brighter green
        panel.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        
        JLabel welcomeLabel = new JLabel("Admin Dashboard - " + username);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcomeLabel.setForeground(Color.WHITE);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutButton.setBackground(new Color(255, 255, 255));
        logoutButton.setForeground(new Color(39, 174, 96));
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
        
        panel.add(welcomeLabel, BorderLayout.WEST);
        panel.add(logoutButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        try {
            // Get user statistics from the database
            Map<String, Object> userStats = dataAccess.getUserStatistics();
            
            // User statistics card with real data
            int activeUsers = (int) userStats.getOrDefault("activeUsers", 0);
            int newUsers = (int) userStats.getOrDefault("newUsers", 0);
            panel.add(createSummaryCard(
                    "User Statistics", 
                    activeUsers + " Active Users", 
                    newUsers + " new users this month",
                    new Color(70, 130, 180)));
        } catch (SQLException e) {
            // Fallback if database error
            panel.add(createSummaryCard(
                    "User Statistics", 
                    "Unable to load", 
                    "Database error: " + e.getMessage(),
                    new Color(70, 130, 180)));
            System.err.println("Error loading user statistics: " + e.getMessage());
        }
        
        // System status card
        panel.add(createSummaryCard(
                "System Status", 
                "Online", 
                "All services running normally",
                new Color(46, 139, 87)));
        
        // Database statistics card
        panel.add(createSummaryCard(
                "Database Status", 
                "Healthy", 
                "Last backup: Today 02:00 AM",
                new Color(218, 165, 32)));
        
        // System resources card
        panel.add(createSummaryCard(
                "System Resources", 
                "24% CPU, 56% Memory", 
                "Storage: 42% used (234GB free)",
                new Color(178, 34, 34)));
        
        return panel;
    }
    
    private JPanel createSummaryCard(String title, String value, String description, Color color) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(color);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setForeground(Color.GRAY);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        panel.add(descLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create user management controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlsPanel.setBackground(new Color(240, 248, 255));
        
        JButton addUserButton = new JButton("Add User");
        styleButton(addUserButton);
        addUserButton.addActionListener(e -> showAddUserDialog());
        
        JButton editUserButton = new JButton("Edit User");
        styleButton(editUserButton);
        editUserButton.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow >= 0) {
                int userId = (int) userTable.getValueAt(selectedRow, 0);
                showEditUserDialog(userId);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Please select a user to edit", 
                    "No Selection", 
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        
        JButton deleteUserButton = new JButton("Delete User");
        styleButton(deleteUserButton);
        deleteUserButton.setBackground(new Color(178, 34, 34)); // Red for delete
        deleteUserButton.addActionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow >= 0) {
                int userId = (int) userTable.getValueAt(selectedRow, 0);
                deleteUser(userId);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Please select a user to delete", 
                    "No Selection", 
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton searchButton = new JButton("Search");
        styleButton(searchButton);
        searchButton.addActionListener(e -> searchUsers(searchField.getText()));
        
        JButton refreshButton = new JButton("Refresh");
        styleButton(refreshButton);
        refreshButton.addActionListener(e -> refreshUserTable());
        
        controlsPanel.add(addUserButton);
        controlsPanel.add(editUserButton);
        controlsPanel.add(deleteUserButton);
        controlsPanel.add(Box.createHorizontalStrut(30));
        controlsPanel.add(searchField);
        controlsPanel.add(searchButton);
        controlsPanel.add(Box.createHorizontalStrut(10));
        controlsPanel.add(refreshButton);
        
        // Create user table
        String[] columnNames = {"ID", "Username", "Email", "Full Name", "Role", "Status", "Last Login"};
        userTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
            
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Integer.class;
                }
                return String.class;
            }
        };
        
        userTable = new JTable(userTableModel);
        userTable.setFont(new Font("Arial", Font.PLAIN, 14));
        userTable.setRowHeight(25);
        userTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane tableScrollPane = new JScrollPane(userTable);
        tableScrollPane.setBorder(BorderFactory.createEtchedBorder());
        
        panel.add(controlsPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Populate the user table with data from the database
     */
    private void refreshUserTable() {
        try {
            // Clear existing data
            userTableModel.setRowCount(0);
            
            // Get users from database
            List<UserAccount> users = dataAccess.getAllUsers();
            
            // Add users to table
            for (UserAccount user : users) {
                Object[] rowData = {
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRoleName(),
                    user.isActive() ? "Active" : "Inactive",
                    user.getLastLogin() != null ? dateFormat.format(user.getLastLogin()) : "Never"
                };
                userTableModel.addRow(rowData);
            }
            
            // Update dashboard statistics
            refreshDashboardStats();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading users: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            System.err.println("Error loading users: " + e.getMessage());
        }
    }
    
    /**
     * Refresh dashboard statistics from the database
     */
    private void refreshDashboardStats() {
        // This will be called from other methods when data changes
        // We'll recreate the dashboard panel to reflect the new data
        if (tabbedPane != null) {
            tabbedPane.setComponentAt(0, createDashboardPanel());
        }
    }
    
    /**
     * Search for users by the given search term
     */
    private void searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            refreshUserTable();
            return;
        }
        
        try {
            // Clear existing data
            userTableModel.setRowCount(0);
            
            // Get matching users from database
            List<UserAccount> users = dataAccess.searchUsers(searchTerm);
            
            // Add users to table
            for (UserAccount user : users) {
                Object[] rowData = {
                    user.getUserId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRoleName(),
                    user.isActive() ? "Active" : "Inactive",
                    user.getLastLogin() != null ? dateFormat.format(user.getLastLogin()) : "Never"
                };
                userTableModel.addRow(rowData);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error searching users: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            System.err.println("Error searching users: " + e.getMessage());
        }
    }
    
    /**
     * Show dialog to add a new user
     */
    private void showAddUserDialog() {
        // Create the dialog
        JDialog dialog = new JDialog(this, "Add New User", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        // Create form panel
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Username field
        JLabel usernameLabel = new JLabel("Username:");
        JTextField usernameField = new JTextField(20);
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        
        // Password field
        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(20);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);
        
        // Email field
        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(20);
        formPanel.add(emailLabel);
        formPanel.add(emailField);
        
        // First name field
        JLabel firstNameLabel = new JLabel("First Name:");
        JTextField firstNameField = new JTextField(20);
        formPanel.add(firstNameLabel);
        formPanel.add(firstNameField);
        
        // Last name field
        JLabel lastNameLabel = new JLabel("Last Name:");
        JTextField lastNameField = new JTextField(20);
        formPanel.add(lastNameLabel);
        formPanel.add(lastNameField);
        
        // Role selector
        JLabel roleLabel = new JLabel("Role:");
        JComboBox<String> roleComboBox = new JComboBox<>(new String[]{"Admin", "NormalUser"});
        formPanel.add(roleLabel);
        formPanel.add(roleComboBox);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        // Add action listeners
        saveButton.addActionListener(e -> {
            try {
                // Validate fields
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                String email = emailField.getText().trim();
                String firstName = firstNameField.getText().trim();
                String lastName = lastNameField.getText().trim();
                String roleName = (String) roleComboBox.getSelectedItem();
                
                if (username.isEmpty() || password.isEmpty() || email.isEmpty() || 
                    firstName.isEmpty() || lastName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "All fields are required", 
                        "Validation Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Register the new user
                UserAccount newUser = UserAccount.register(
                    username, password, email, firstName, lastName, roleName);
                
                if (newUser != null) {
                    JOptionPane.showMessageDialog(dialog, 
                        "User created successfully", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    refreshUserTable();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, 
                        "Failed to create user. Username or email may already exist.", 
                        "Registration Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Database error: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.err.println("Error creating user: " + ex.getMessage());
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        // Add panels to dialog
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        // Show dialog
        dialog.setVisible(true);
    }
    
    /**
     * Show dialog to edit an existing user
     */
    private void showEditUserDialog(int userId) {
        try {
            // Get all users to find the one to edit
            List<UserAccount> allUsers = dataAccess.getAllUsers();
            UserAccount userToEdit = null;
            
            for (UserAccount user : allUsers) {
                if (user.getUserId() == userId) {
                    userToEdit = user;
                    break;
                }
            }
            
            if (userToEdit == null) {
                JOptionPane.showMessageDialog(this, 
                    "User not found", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create the dialog
            JDialog dialog = new JDialog(this, "Edit User", true);
            dialog.setSize(450, 350);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());
            
            // Create form panel
            JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            // Username field (disabled as it shouldn't change)
            JLabel usernameLabel = new JLabel("Username:");
            JTextField usernameField = new JTextField(userToEdit.getUsername());
            usernameField.setEditable(false);
            formPanel.add(usernameLabel);
            formPanel.add(usernameField);
            
            // Email field
            JLabel emailLabel = new JLabel("Email:");
            JTextField emailField = new JTextField(userToEdit.getEmail());
            formPanel.add(emailLabel);
            formPanel.add(emailField);
            
            // First name field
            JLabel firstNameLabel = new JLabel("First Name:");
            JTextField firstNameField = new JTextField(userToEdit.getFirstName());
            formPanel.add(firstNameLabel);
            formPanel.add(firstNameField);
            
            // Last name field
            JLabel lastNameLabel = new JLabel("Last Name:");
            JTextField lastNameField = new JTextField(userToEdit.getLastName());
            formPanel.add(lastNameLabel);
            formPanel.add(lastNameField);
            
            // Role selector
            JLabel roleLabel = new JLabel("Role:");
            JComboBox<String> roleComboBox = new JComboBox<>(new String[]{"Admin", "NormalUser"});
            roleComboBox.setSelectedItem(userToEdit.getRoleName());
            formPanel.add(roleLabel);
            formPanel.add(roleComboBox);
            
            // Status selector
            JLabel statusLabel = new JLabel("Status:");
            JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"Active", "Inactive"});
            statusComboBox.setSelectedItem(userToEdit.isActive() ? "Active" : "Inactive");
            formPanel.add(statusLabel);
            formPanel.add(statusComboBox);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton saveButton = new JButton("Save");
            JButton cancelButton = new JButton("Cancel");
            
            buttonPanel.add(cancelButton);
            buttonPanel.add(saveButton);
            
            // Add action listeners
            final UserAccount finalUserToEdit = userToEdit;
            saveButton.addActionListener(e -> {
                try {
                    // Validate fields
                    String email = emailField.getText().trim();
                    String firstName = firstNameField.getText().trim();
                    String lastName = lastNameField.getText().trim();
                    String roleName = (String) roleComboBox.getSelectedItem();
                    boolean isActive = statusComboBox.getSelectedItem().equals("Active");
                    
                    if (email.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, 
                            "All fields are required", 
                            "Validation Error", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Get role ID
                    int roleId = UserAccount.getRoleId(roleName);
                    
                    // Update the user
                    boolean updated = dataAccess.updateUser(
                        finalUserToEdit.getUserId(), email, firstName, lastName, roleId, isActive);
                    
                    if (updated) {
                        JOptionPane.showMessageDialog(dialog, 
                            "User updated successfully", 
                            "Success", 
                            JOptionPane.INFORMATION_MESSAGE);
                        refreshUserTable();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, 
                            "Failed to update user.", 
                            "Update Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Database error: " + ex.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    System.err.println("Error updating user: " + ex.getMessage());
                }
            });
            
            cancelButton.addActionListener(e -> dialog.dispose());
            
            // Add panels to dialog
            dialog.add(formPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
            // Show dialog
            dialog.setVisible(true);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading user data: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            System.err.println("Error loading user data: " + e.getMessage());
        }
    }
    
    /**
     * Delete a user
     */
    private void deleteUser(int userId) {
        // Confirm deletion
        int response = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this user?",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (response == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = dataAccess.deleteUser(userId);
                
                if (deleted) {
                    JOptionPane.showMessageDialog(this, 
                        "User deleted successfully", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    refreshUserTable();
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Failed to delete user.", 
                        "Deletion Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, 
                    "Database error: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.err.println("Error deleting user: " + e.getMessage());
            }
        }
    }
    
    private JPanel createSystemSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create settings sections
        JTabbedPane settingsTabs = new JTabbedPane();
        settingsTabs.setFont(new Font("Arial", Font.BOLD, 14));
        
        // General settings
        JPanel generalPanel = createSettingsSection(
                new String[]{"Application Name", "Maintenance Mode", "Session Timeout (minutes)", "Max Upload Size (MB)"}, 
                new String[]{"Carbon Emissions Management System", "Off", "30", "10"});
        settingsTabs.addTab("General", generalPanel);
        
        // Security settings
        JPanel securityPanel = createSettingsSection(
                new String[]{"Password Minimum Length", "Password Requires Special Char", "Account Lockout Attempts", "Two-Factor Authentication"}, 
                new String[]{"8", "Yes", "5", "Optional"});
        settingsTabs.addTab("Security", securityPanel);
        
        // Email settings
        JPanel emailPanel = createSettingsSection(
                new String[]{"SMTP Server", "SMTP Port", "Sender Email", "Email Encryption"}, 
                new String[]{"smtp.example.com", "587", "noreply@example.com", "TLS"});
        settingsTabs.addTab("Email", emailPanel);
        
        // Backup settings
        JPanel backupPanel = createSettingsSection(
                new String[]{"Automatic Backups", "Backup Frequency", "Backup Retention (days)", "Backup Location"}, 
                new String[]{"Enabled", "Daily", "30", "/var/backups/carbon_emissions"});
        settingsTabs.addTab("Backup", backupPanel);
        
        panel.add(settingsTabs, BorderLayout.CENTER);
        
        // Save button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton saveButton = new JButton("Save All Settings");
        styleButton(saveButton);
        
        JButton resetButton = new JButton("Reset to Defaults");
        resetButton.setFont(new Font("Arial", Font.BOLD, 14));
        resetButton.setBackground(new Color(220, 220, 220));
        resetButton.setForeground(Color.BLACK);
        resetButton.setFocusPainted(false);
        resetButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        buttonPanel.add(resetButton);
        buttonPanel.add(saveButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSettingsSection(String[] labels, String[] values) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i] + ":");
            label.setFont(new Font("Arial", Font.BOLD, 14));
            
            JTextField field = new JTextField(values[i]);
            field.setFont(new Font("Arial", Font.PLAIN, 14));
            
            panel.add(label);
            panel.add(field);
        }
        
        return panel;
    }
    
    private JPanel createDatabasePanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Database connection panel
        JPanel connectionPanel = new JPanel(new GridLayout(0, 2, 15, 15));
        connectionPanel.setBackground(new Color(240, 248, 255));
        connectionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Database Connection", 
                TitledBorder.LEFT, 
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)));
        
        String[] connLabels = {"Database Type", "Host", "Port", "Database Name", "Username", "Password"};
        String[] connValues = {"MySQL", "localhost", "3306", "carbon_emissions_db", "root", "********"};
        
        for (int i = 0; i < connLabels.length; i++) {
            JLabel label = new JLabel(connLabels[i] + ":");
            label.setFont(new Font("Arial", Font.BOLD, 14));
            
            JTextField field = new JTextField(connValues[i]);
            field.setFont(new Font("Arial", Font.PLAIN, 14));
            
            connectionPanel.add(label);
            connectionPanel.add(field);
        }
        
        // Database management buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton testConnButton = new JButton("Test Connection");
        styleButton(testConnButton);
        
        JButton backupButton = new JButton("Backup Database");
        styleButton(backupButton);
        
        JButton restoreButton = new JButton("Restore Database");
        styleButton(restoreButton);
        
        JButton optimizeButton = new JButton("Optimize Database");
        styleButton(optimizeButton);
        
        buttonPanel.add(testConnButton);
        buttonPanel.add(backupButton);
        buttonPanel.add(restoreButton);
        buttonPanel.add(optimizeButton);
        
        // Database status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Database Status", 
                TitledBorder.LEFT, 
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)));
        
        JTextArea statusArea = new JTextArea();
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        statusArea.setEditable(false);
        statusArea.setText("Connected to: MySQL Server 8.0.33\n" +
                "Database: carbon_emissions_db\n" +
                "Status: Connected\n" +
                "Tables: 12\n" +
                "Size: 24.5 MB\n" +
                "Last Backup: 2023-06-02 02:00 AM");
        
        JScrollPane statusScrollPane = new JScrollPane(statusArea);
        statusPanel.add(statusScrollPane, BorderLayout.CENTER);
        
        // Add components to main panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 248, 255));
        topPanel.add(connectionPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(statusPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void styleButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(new Color(39, 174, 96)); // Brighter green
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    private void logout() {
        try {
            SessionManager.getInstance().logout();
            MainLoginFrame loginFrame = new MainLoginFrame();
            loginFrame.setVisible(true);
            this.dispose();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error during logout: " + e.getMessage(),
                    "Logout Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
} 