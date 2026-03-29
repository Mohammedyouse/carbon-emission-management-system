package com.carbon.emissions.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

import com.carbon.emissions.auth.UserAccount;
import com.carbon.emissions.auth.SessionManager;

/**
 * Main login frame for the Carbon Emissions Management System
 */
public class MainLoginFrame extends JFrame {
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private CardLayout cardLayout;
    private JPanel cardPanel;
    
    // Components for registration
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;
    private JTextField regEmailField;
    private JTextField regFirstNameField;
    private JTextField regLastNameField;
    private JComboBox<String> regRoleComboBox;
    
    public MainLoginFrame() {
        // Set up the frame
        setTitle("Carbon Emissions Management System");
        setSize(550, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen
        
        // Create components with modern look
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(new Color(245, 250, 255)); // Light blue background
        
        // Header panel with logo
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 250, 255)); // Same color as main panel
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        JLabel titleLabel = new JLabel("Carbon Emissions Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(39, 174, 96)); // Brighter green text
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Try to load the icon, but provide a text fallback if it's not found
        JLabel iconLabel = new JLabel();
        try {
            // First try with class loader
            ImageIcon icon = new ImageIcon(getClass().getResource("/tree_icon.png"));
            if (icon.getIconWidth() <= 0) {
                // If failed, try absolute path in resources directory
                icon = new ImageIcon(getClass().getResource("/com/carbon/emissions/ui/resources/tree_icon.png"));
            }
            
            // If icon still not loaded, use emoji fallback
            if (icon.getIconWidth() <= 0) {
                iconLabel.setText("🌳");
                iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 60));
                iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                iconLabel.setIcon(icon);
                iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            }
        } catch (Exception e) {
            // Fallback to emoji
            iconLabel.setText("🌳");
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 60));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        
        headerPanel.add(iconLabel, BorderLayout.CENTER);
        headerPanel.add(titleLabel, BorderLayout.SOUTH);
        
        // Create card panel for login and registration
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(new Color(245, 250, 255));
        
        // Create login panel
        JPanel loginPanel = createLoginPanel();
        
        // Create registration panel
        JPanel registerPanel = createRegisterPanel();
        
        // Add panels to card layout
        cardPanel.add(loginPanel, "login");
        cardPanel.add(registerPanel, "register");
        
        // Add all panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Show login by default
        cardLayout.show(cardPanel, "login");
    }
    
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 250, 255));
        
        // Create form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(3, 2, 15, 20));
        formPanel.setBackground(new Color(245, 250, 255));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));
        
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            usernameField.getBorder(),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)
        ));
        
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            passwordField.getBorder(),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)
        ));
        
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        String[] roles = {"NormalUser", "Admin", "Analyst"};
        roleComboBox = new JComboBox<>(roles);
        roleComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);
        formPanel.add(roleLabel);
        formPanel.add(roleComboBox);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(245, 250, 255));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        
        JButton loginButton = new JButton("Login");
        styleButton(loginButton);
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        MainLoginFrame.this,
                        "Please enter both username and password",
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                
                try {
                    UserAccount user = UserAccount.authenticate(username, password);
                    
                    if (user != null) {
                        // Set the current user in the session manager
                        SessionManager.getInstance().setCurrentUser(user);
                        
                        // Close login window
                        MainLoginFrame.this.dispose();
                        
                        // Open appropriate dashboard based on role
                        String roleName = user.getRoleName();
                        if ("Admin".equals(roleName)) {
                            new AdminDashboard(username).setVisible(true);
                        } else if ("Analyst".equals(roleName)) {
                            new AnalysisDashboard(username).setVisible(true);
                        } else {
                            new UserDashboard(username).setVisible(true);
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                            MainLoginFrame.this,
                            "Invalid username or password",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(
                        MainLoginFrame.this,
                        "Database error: " + ex.getMessage(),
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    System.err.println("Login error: " + ex.getMessage());
                }
            }
        });
        
        JButton registerButton = new JButton("Create Account");
        styleButton(registerButton);
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Switch to registration form
                cardLayout.show(cardPanel, "register");
            }
        });
        
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        
        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 250, 255));
        
        // Form title
        JLabel titleLabel = new JLabel("Create New Account", JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        
        // Create form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(7, 2, 10, 15));
        formPanel.setBackground(new Color(245, 250, 255));
        formPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 20, 40));
        
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        regUsernameField = new JTextField();
        regUsernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        regPasswordField = new JPasswordField();
        regPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        regConfirmPasswordField = new JPasswordField();
        regConfirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        regEmailField = new JTextField();
        regEmailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel firstNameLabel = new JLabel("First Name:");
        firstNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        regFirstNameField = new JTextField();
        regFirstNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel lastNameLabel = new JLabel("Last Name:");
        lastNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        regLastNameField = new JTextField();
        regLastNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        String[] roles = {"NormalUser", "Admin"};
        regRoleComboBox = new JComboBox<>(roles);
        regRoleComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        formPanel.add(usernameLabel);
        formPanel.add(regUsernameField);
        formPanel.add(passwordLabel);
        formPanel.add(regPasswordField);
        formPanel.add(confirmLabel);
        formPanel.add(regConfirmPasswordField);
        formPanel.add(emailLabel);
        formPanel.add(regEmailField);
        formPanel.add(firstNameLabel);
        formPanel.add(regFirstNameField);
        formPanel.add(lastNameLabel);
        formPanel.add(regLastNameField);
        formPanel.add(roleLabel);
        formPanel.add(regRoleComboBox);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(new Color(245, 250, 255));
        
        JButton registerButton = new JButton("Register");
        styleButton(registerButton);
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                register();
            }
        });
        
        JButton backButton = new JButton("Back to Login");
        styleButton(backButton);
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Switch back to login
                cardLayout.show(cardPanel, "login");
            }
        });
        
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);
        
        // Add panels to registration panel
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
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
    
    private void register() {
        // Get registration data
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirmPassword = new String(regConfirmPasswordField.getPassword());
        String email = regEmailField.getText().trim();
        String firstName = regFirstNameField.getText().trim();
        String lastName = regLastNameField.getText().trim();
        String role = (String) regRoleComboBox.getSelectedItem();
        
        // Validate input
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || 
            firstName.isEmpty() || lastName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validate email format
        if (!email.contains("@") || !email.contains(".")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Register new user
            UserAccount newUser = UserAccount.register(username, password, email, firstName, lastName, role);
            
            if (newUser != null) {
                JOptionPane.showMessageDialog(this, "Registration successful! You can now login.", 
                        "Registration Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Clear registration fields
                regUsernameField.setText("");
                regPasswordField.setText("");
                regConfirmPasswordField.setText("");
                regEmailField.setText("");
                regFirstNameField.setText("");
                regLastNameField.setText("");
                
                // Switch back to login
                cardLayout.show(cardPanel, "login");
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed. Username or email may already exist.", 
                        "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        // Use system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and display the login form
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainLoginFrame().setVisible(true);
            }
        });
    }
} 