package com.carbon.emissions.ui;

import com.carbon.emissions.CarbonEmissionsDBConnection;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

/**
 * Dashboard for regular users to view and track carbon emissions
 */
public class UserDashboard extends JFrame {
    
    // Simple interface to handle document changes
    private interface SimpleDocumentListener extends DocumentListener {
        void update(DocumentEvent e);
        
        @Override
        default void insertUpdate(DocumentEvent e) {
            update(e);
        }
        
        @Override
        default void removeUpdate(DocumentEvent e) {
            update(e);
        }
        
        @Override
        default void changedUpdate(DocumentEvent e) {
            update(e);
        }
    }
    
    private String username;
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    
    // Input fields
    private JTextField businessNameField;
    private JTextField businessSectorField;
    private JTextField currentEmissionsField;
    private JTextField targetEmissionsField;
    private JTextField energyAmountField;
    
    // Display fields for calculated values
    private JLabel implementationCostLabel;
    private JLabel paybackPeriodLabel;
    private JLabel annualSavingsLabel;
    
    // Energy source components
    private JComboBox<String> currentEnergySourceCombo;
    private JComboBox<String> newEnergySourceCombo;
    
    // Energy source data maps
    private Map<Integer, String> energySources = new HashMap<>();
    private Map<String, Integer> energySourceIds = new HashMap<>();
    private Map<Integer, Double> carbonFactors = new HashMap<>();
    private Map<Integer, Boolean> renewableFlags = new HashMap<>();
    private Map<Integer, Double> energyCosts = new HashMap<>();  // Cost per unit of energy
    private Map<Integer, Double> implementationCosts = new HashMap<>();  // Cost to implement per unit
    
    // Result fields
    private JLabel footprintValueLabel;
    private JLabel footprintDescLabel;
    private JLabel energyValueLabel;
    private JLabel energyDescLabel;
    private JLabel costValueLabel;
    private JLabel costDescLabel;
    private JLabel targetValueLabel;
    private JLabel targetDescLabel;
    
    // Business ID from database
    private int businessId = 1; // Default to ID 1 for demo purposes
    
    // Result panels
    private JPanel comparisonPanel;
    
    // Tabs
    private JPanel dashboardPanel;
    private JPanel detailsPanel;
    private JPanel settingsPanel;
    
    // Add fields to store references to UI components for updates
    private JTable metricsTable;
    private JLabel businessNameValueLabel;
    private JLabel transitionValueLabel;
    private JPanel initialMessagePanel;
    private JPanel detailsContentPanel;
    
    // Reference to logged-in user account
    private com.carbon.emissions.auth.UserAccount userAccount;
    
    // Add a field to track document listeners
    private final java.util.List<DocumentListener> documentListeners = new java.util.ArrayList<>();
    
    public UserDashboard(String username) {
        this.username = username;
        
        // Get user from session
        this.userAccount = com.carbon.emissions.auth.SessionManager.getInstance().getCurrentUser();
        
        // If user account available, use their business ID if they have one
        if (userAccount != null && userAccount.getBusinessId() > 0) {
            this.businessId = userAccount.getBusinessId();
        }
        
        // Set up the frame
        setTitle("User Dashboard - Carbon Emissions Management");
        setSize(900, 700);
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
        
        // Create the panels for each tab
        dashboardPanel = createDashboardPanel();
        detailsPanel = createDetailsPanel();
        settingsPanel = createSettingsPanel();
        
        // Add tabs
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Details", detailsPanel);
        tabbedPane.addTab("Settings", settingsPanel);
        
        // Add tab change listener to ensure fields are initialized
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 0) { // Dashboard tab
                SwingUtilities.invokeLater(this::ensureDashboardFieldsInitialized);
            }
        });
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Initialize dashboard with existing data if available
        loadBusinessData();
        
        // Make sure fields are initialized after loading
        SwingUtilities.invokeLater(() -> {
            ensureDashboardFieldsInitialized();
        });
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(39, 174, 96)); // Brighter green
        panel.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        
        JLabel welcomeLabel = new JLabel("Welcome, " + username);
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
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(245, 250, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Input panel for business and emission details
        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        inputPanel.setBackground(new Color(240, 248, 255));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Enter Business & Energy Details",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16)));
        
        // Business information fields
        JLabel businessNameLabel = new JLabel("Business Name:");
        businessNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        businessNameField = new JTextField("EcoTech Solutions");
        
        JLabel businessSectorLabel = new JLabel("Business Sector:");
        businessSectorLabel.setFont(new Font("Arial", Font.BOLD, 14));
        businessSectorField = new JTextField("Technology");
        
        // Load energy sources from database
        loadEnergySources();
        
        // Energy source transition fields
        JLabel currentEnergySourceLabel = new JLabel("Current Energy Source:");
        currentEnergySourceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentEnergySourceCombo = new JComboBox<>();
        
        JLabel newEnergySourceLabel = new JLabel("Proposed Energy Source:");
        newEnergySourceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        newEnergySourceCombo = new JComboBox<>();
        
        // Filter and load energy sources into the combo boxes
        filterEnergySourcesForDropdown();
        
        JLabel energyAmountLabel = new JLabel("Energy Amount (kWh per year):");
        energyAmountLabel.setFont(new Font("Arial", Font.BOLD, 14));
        energyAmountField = new JTextField("1000000");
        
        // Calculated values display
        JLabel implementationCostDescLabel = new JLabel("Implementation Cost ($):");
        implementationCostDescLabel.setFont(new Font("Arial", Font.BOLD, 14));
        implementationCostLabel = new JLabel("(calculated)");
        implementationCostLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel paybackPeriodDescLabel = new JLabel("Payback Period (months):");
        paybackPeriodDescLabel.setFont(new Font("Arial", Font.BOLD, 14));
        paybackPeriodLabel = new JLabel("(calculated)");
        paybackPeriodLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel annualSavingsDescLabel = new JLabel("Annual Savings ($):");
        annualSavingsDescLabel.setFont(new Font("Arial", Font.BOLD, 14));
        annualSavingsLabel = new JLabel("(calculated)");
        annualSavingsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Emission information fields
        JLabel currentEmissionsLabel = new JLabel("Current Emissions (tons CO2e):");
        currentEmissionsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentEmissionsField = new JTextField("(calculated)");
        currentEmissionsField.setEditable(false);
        
        JLabel targetEmissionsLabel = new JLabel("Target Emissions (tons CO2e):");
        targetEmissionsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        targetEmissionsField = new JTextField("(calculated)");
        targetEmissionsField.setEditable(false);
        
        // Add auto-calculation listeners
        currentEnergySourceCombo.addActionListener(e -> calculateValues());
        newEnergySourceCombo.addActionListener(e -> calculateValues());
        energyAmountField.addActionListener(e -> calculateValues());
        DocumentListener energyAmountListener = new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                calculateValues();
            }
        };
        documentListeners.add(energyAmountListener);
        energyAmountField.getDocument().addDocumentListener(energyAmountListener);
        
        // Add components to input panel
        inputPanel.add(businessNameLabel);
        inputPanel.add(businessNameField);
        inputPanel.add(businessSectorLabel);
        inputPanel.add(businessSectorField);
        inputPanel.add(currentEnergySourceLabel);
        inputPanel.add(currentEnergySourceCombo);
        inputPanel.add(newEnergySourceLabel);
        inputPanel.add(newEnergySourceCombo);
        inputPanel.add(energyAmountLabel);
        inputPanel.add(energyAmountField);
        inputPanel.add(implementationCostDescLabel);
        inputPanel.add(implementationCostLabel);
        inputPanel.add(annualSavingsDescLabel);
        inputPanel.add(annualSavingsLabel);
        inputPanel.add(paybackPeriodDescLabel);
        inputPanel.add(paybackPeriodLabel);
        inputPanel.add(currentEmissionsLabel);
        inputPanel.add(currentEmissionsField);
        inputPanel.add(targetEmissionsLabel);
        inputPanel.add(targetEmissionsField);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton calculateButton = new JButton("Calculate & Save");
        styleButton(calculateButton);
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndCalculate();
                // Switch to the Details tab to show results
                tabbedPane.setSelectedIndex(1);
            }
        });
        
        buttonPanel.add(calculateButton);
        
        // Create comparison panel for showing side-by-side comparison of energy sources
        comparisonPanel = createComparisonPanel();
        comparisonPanel.setVisible(false); // Initially hidden until calculation
        
        // Main content panel combining input, comparison and results
        JPanel contentPanel = new JPanel(new BorderLayout(0, 20));
        contentPanel.setBackground(new Color(240, 248, 255));
        contentPanel.add(inputPanel, BorderLayout.NORTH);
        contentPanel.add(buttonPanel, BorderLayout.CENTER);
        
        // Container for comparison panel
        JPanel comparisonContainer = new JPanel(new BorderLayout(0, 15));
        comparisonContainer.setBackground(new Color(240, 248, 255));
        comparisonContainer.add(comparisonPanel, BorderLayout.CENTER);
        
        contentPanel.add(comparisonContainer, BorderLayout.SOUTH);
        
        // Add all panels to dashboard panel
        panel.add(contentPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create a summary panel at the top
        JPanel summaryPanel = new JPanel(new BorderLayout(0, 10));
        summaryPanel.setBackground(new Color(240, 248, 255));
        summaryPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Energy Transition Summary",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16)));
        
        // Business and transition info
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        infoPanel.setBackground(new Color(240, 248, 255));
        
        // Display business info and energy sources
        JLabel businessNameLabel = new JLabel("Business Name:");
        businessNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel businessNameValueLabel = new JLabel("-");
        businessNameValueLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel transitionLabel = new JLabel("Energy Transition:");
        transitionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        JLabel transitionValueLabel = new JLabel("-");
        transitionValueLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        infoPanel.add(businessNameLabel);
        infoPanel.add(businessNameValueLabel);
        infoPanel.add(transitionLabel);
        infoPanel.add(transitionValueLabel);
        
        summaryPanel.add(infoPanel, BorderLayout.NORTH);
        
        // Results panel with cards showing calculated values
        JPanel resultsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        resultsPanel.setBackground(new Color(240, 248, 255));
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Calculated Results",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16)));
        
        // Create result cards with empty values to be populated after calculation
        JPanel footprintCard = createResultCard(
                "Carbon Footprint",
                "--",
                "--",
                new Color(46, 139, 87));
        footprintValueLabel = (JLabel) ((JPanel)((BorderLayout)footprintCard.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getComponent(0);
        footprintDescLabel = (JLabel) ((JPanel)((BorderLayout)footprintCard.getLayout()).getLayoutComponent(BorderLayout.SOUTH)).getComponent(0);
        
        JPanel energyCard = createResultCard(
                "Energy Consumption",
                "--",
                "--",
                new Color(70, 130, 180));
        energyValueLabel = (JLabel) ((JPanel)((BorderLayout)energyCard.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getComponent(0);
        energyDescLabel = (JLabel) ((JPanel)((BorderLayout)energyCard.getLayout()).getLayoutComponent(BorderLayout.SOUTH)).getComponent(0);
        
        JPanel costCard = createResultCard(
                "Cost Savings",
                "--",
                "--",
                new Color(218, 165, 32));
        costValueLabel = (JLabel) ((JPanel)((BorderLayout)costCard.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getComponent(0);
        costDescLabel = (JLabel) ((JPanel)((BorderLayout)costCard.getLayout()).getLayoutComponent(BorderLayout.SOUTH)).getComponent(0);
        
        JPanel targetCard = createResultCard(
                "Reduction Targets",
                "--",
                "--",
                new Color(178, 34, 34));
        targetValueLabel = (JLabel) ((JPanel)((BorderLayout)targetCard.getLayout()).getLayoutComponent(BorderLayout.CENTER)).getComponent(0);
        targetDescLabel = (JLabel) ((JPanel)((BorderLayout)targetCard.getLayout()).getLayoutComponent(BorderLayout.SOUTH)).getComponent(0);
        
        resultsPanel.add(footprintCard);
        resultsPanel.add(energyCard);
        resultsPanel.add(costCard);
        resultsPanel.add(targetCard);
        
        // Add a detailed metrics panel
        JPanel metricsPanel = new JPanel(new BorderLayout());
        metricsPanel.setBackground(new Color(240, 248, 255));
        metricsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Detailed Metrics",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16)));
        
        // Create table for detailed metrics
        String[] columnNames = {"Metric", "Current", "Proposed", "Difference", "% Change"};
        Object[][] data = {
            {"Energy Source", "-", "-", "-", "-"},
            {"Implementation Cost ($)", "-", "-", "-", "-"},
            {"Annual Energy Cost ($)", "-", "-", "-", "-"},
            {"Annual Savings ($)", "-", "-", "-", "-"},
            {"Payback Period (months)", "-", "-", "-", "-"},
            {"Carbon Emissions (tons CO2e/year)", "-", "-", "-", "-"},
            {"Emissions Reduction (%)", "-", "-", "-", "-"}
        };
        
        JTable metricsTable = new JTable(data, columnNames);
        metricsTable.setRowHeight(30);
        metricsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        metricsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        metricsTable.setEnabled(false);  // Make table non-editable
        
        // Customize table appearance
        metricsTable.setShowGrid(true);
        metricsTable.setGridColor(Color.LIGHT_GRAY);
        
        // Center align all cells
        for (int i = 0; i < metricsTable.getColumnCount(); i++) {
            metricsTable.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer() {
                {
                    setHorizontalAlignment(JLabel.CENTER);
                }
            });
        }
        
        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(metricsTable);
        metricsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Save references for later updates
        this.metricsTable = metricsTable;
        this.businessNameValueLabel = businessNameValueLabel;
        this.transitionValueLabel = transitionValueLabel;
        
        // Add panels to the main details panel
        JPanel topPanel = new JPanel(new BorderLayout(0, 15));
        topPanel.setBackground(new Color(240, 248, 255));
        topPanel.add(summaryPanel, BorderLayout.NORTH);
        topPanel.add(resultsPanel, BorderLayout.CENTER);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(metricsPanel, BorderLayout.CENTER);
        
        // Initial message
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(new Color(240, 248, 255));
        JLabel messageLabel = new JLabel("Please enter data and click 'Calculate & Save' on the Dashboard tab to see results.", JLabel.CENTER);
        messageLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        messageLabel.setForeground(Color.GRAY);
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        
        this.initialMessagePanel = messagePanel;
        this.detailsContentPanel = panel;
        
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(new Color(240, 248, 255));
        wrapperPanel.add(messagePanel, BorderLayout.CENTER);
        
        return wrapperPanel;
    }
    
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Settings form
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 15));
        formPanel.setBackground(new Color(240, 248, 255));
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "User Settings", 
                TitledBorder.LEFT, 
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 14)));
        
        // Profile settings
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JTextField nameField = new JTextField(username);
        
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JTextField emailField = new JTextField("user@example.com");
        
        JLabel passwordLabel = new JLabel("New Password:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JPasswordField passwordField = new JPasswordField();
        
        JLabel confirmLabel = new JLabel("Confirm Password:");
        confirmLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JPasswordField confirmField = new JPasswordField();
        
        JLabel notificationLabel = new JLabel("Email Notifications:");
        notificationLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JCheckBox notificationCheck = new JCheckBox("Receive email reports and alerts");
        notificationCheck.setSelected(true);
        notificationCheck.setBackground(new Color(240, 248, 255));
        
        JLabel emptyLabel = new JLabel("");
        
        JButton saveButton = new JButton("Save Changes");
        styleButton(saveButton);
        
        formPanel.add(nameLabel);
        formPanel.add(nameField);
        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);
        formPanel.add(confirmLabel);
        formPanel.add(confirmField);
        formPanel.add(notificationLabel);
        formPanel.add(notificationCheck);
        formPanel.add(emptyLabel);
        formPanel.add(saveButton);
        
        panel.add(formPanel, BorderLayout.NORTH);
        
        return panel;
    }
    
    /**
     * Load energy sources from the database
     */
    private void loadEnergySources() {
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT es.source_id, es.source_name, es.carbon_factor, es.is_renewable, " +
                 "COALESCE(AVG(bes.unit_cost), 0) as avg_unit_cost " +
                 "FROM EnergySource es " +
                 "LEFT JOIN BusinessEnergySource bes ON es.source_id = bes.source_id " +
                 "GROUP BY es.source_id, es.source_name, es.carbon_factor, es.is_renewable")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("source_id");
                String name = rs.getString("source_name");
                double factor = rs.getDouble("carbon_factor");
                boolean renewable = rs.getBoolean("is_renewable");
                double cost = rs.getDouble("avg_unit_cost");
                
                // Skip Nuclear, Geothermal, and Tidal
                if (name.equals("Nuclear") || name.equals("Geothermal") || name.equals("Tidal")) {
                    continue;
                }
                
                energySources.put(id, name);
                energySourceIds.put(name, id);
                carbonFactors.put(id, factor);
                renewableFlags.put(id, renewable);
                energyCosts.put(id, cost);
                
                // Set implementation costs based on energy type (would come from database in real app)
                if (renewable) {
                    implementationCosts.put(id, id == 3 ? 1500.0 : 2000.0); // Solar costs $1500 per kW, others $2000
                } else {
                    implementationCosts.put(id, 800.0); // Non-renewable sources cost $800 per kW to implement
                }
            }
            
            // If no sources found, add some defaults
            if (energySources.isEmpty()) {
                setupDefaultEnergySources();
            }
        } catch (SQLException e) {
            System.err.println("Error loading energy sources: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Could not load energy sources. Using default values.\nError: " + e.getMessage(),
                "Database Error", JOptionPane.WARNING_MESSAGE);
            
            // Set default values if database fails
            setupDefaultEnergySources();
        }
    }
    
    /**
     * Set up default energy sources for when database is unavailable
     */
    private void setupDefaultEnergySources() {
        energySources.put(1, "Coal");
        energySources.put(2, "Natural Gas");
        energySources.put(3, "Solar");
        energySources.put(4, "Wind");
        energySources.put(5, "Hydroelectric");
        energySources.put(6, "Biomass");
        energySources.put(7, "Oil");
        // Remove Nuclear, Geothermal, and Tidal as per requirement
        
        energySourceIds.put("Coal", 1);
        energySourceIds.put("Natural Gas", 2);
        energySourceIds.put("Solar", 3);
        energySourceIds.put("Wind", 4);
        energySourceIds.put("Hydroelectric", 5);
        energySourceIds.put("Biomass", 6);
        energySourceIds.put("Oil", 7);
        
        carbonFactors.put(1, 0.909);  // kg CO2 per kWh
        carbonFactors.put(2, 0.386);  // kg CO2 per kWh
        carbonFactors.put(3, 0.045);  // kg CO2 per kWh
        carbonFactors.put(4, 0.011);  // kg CO2 per kWh
        carbonFactors.put(5, 0.024);  // kg CO2 per kWh
        carbonFactors.put(6, 0.230);  // kg CO2 per kWh for Biomass
        carbonFactors.put(7, 0.840);  // kg CO2 per kWh for Oil
        
        renewableFlags.put(1, false);
        renewableFlags.put(2, false);
        renewableFlags.put(3, true);
        renewableFlags.put(4, true);
        renewableFlags.put(5, true);
        renewableFlags.put(6, true);   // Biomass is renewable
        renewableFlags.put(7, false);  // Oil is not renewable
        
        energyCosts.put(1, 0.15);  // $ per kWh for coal
        energyCosts.put(2, 0.12);  // $ per kWh for natural gas
        energyCosts.put(3, 0.08);  // $ per kWh for solar
        energyCosts.put(4, 0.07);  // $ per kWh for wind
        energyCosts.put(5, 0.07);  // $ per kWh for hydroelectric
        energyCosts.put(6, 0.10);  // $ per kWh for biomass
        energyCosts.put(7, 0.18);  // $ per kWh for oil
        
        // Implementation costs per kW
        implementationCosts.put(1, 800.0);  // $ per kW for coal
        implementationCosts.put(2, 600.0);  // $ per kW for natural gas
        implementationCosts.put(3, 1500.0); // $ per kW for solar
        implementationCosts.put(4, 1800.0); // $ per kW for wind
        implementationCosts.put(5, 2500.0); // $ per kW for hydroelectric
        implementationCosts.put(6, 1900.0); // $ per kW for biomass
        implementationCosts.put(7, 700.0);  // $ per kW for oil
    }
    
    /**
     * Calculate all values - emissions, costs, payback period
     */
    private void calculateValues() {
        try {
            // First calculate emissions
            calculateEmissions();
            
            // Then calculate financial metrics
            calculateFinancialMetrics();
            
            System.out.println("Calculated values:");
            System.out.println("- Current source: " + currentEnergySourceCombo.getSelectedItem());
            System.out.println("- New source: " + newEnergySourceCombo.getSelectedItem());
            System.out.println("- Energy amount: " + energyAmountField.getText());
            
        } catch (Exception e) {
            System.err.println("Error calculating values: " + e.getMessage());
        }
    }
    
    /**
     * Calculate emissions based on selected energy sources and amount
     */
    private void calculateEmissions() {
        try {
            // Get selected energy sources
            String currentSourceName = (String) currentEnergySourceCombo.getSelectedItem();
            String newSourceName = (String) newEnergySourceCombo.getSelectedItem();
            
            if (currentSourceName == null || newSourceName == null) {
                return;
            }
            
            // Get the energy source IDs
            int currentSourceId = energySourceIds.get(currentSourceName);
            int newSourceId = energySourceIds.get(newSourceName);
            
            // Get energy amount
            double energyAmount = 0;
            try {
                energyAmount = Double.parseDouble(energyAmountField.getText());
            } catch (NumberFormatException e) {
                // Invalid number, do nothing
                return;
            }
            
            // Calculate emissions using carbon factors
            double currentEmissions = energyAmount * carbonFactors.get(currentSourceId) / 1000; // Convert kg to tons
            double targetEmissions = energyAmount * carbonFactors.get(newSourceId) / 1000; // Convert kg to tons
            
            // Update the fields
            currentEmissionsField.setText(String.format("%.2f", currentEmissions));
            targetEmissionsField.setText(String.format("%.2f", targetEmissions));
            
            // Update the comparison panel
            updateComparisonPanel(currentSourceName, newSourceName, energyAmount);
            
            // Also update dashboard results in real-time for preview
            double emissionsReduction = currentEmissions - targetEmissions;
            double reductionPercentage = (emissionsReduction / currentEmissions) * 100;
            
            // Get financial metrics for dashboard update
            double annualSavings = 0;
            try {
                // Parse from the annual savings label
                String savingsText = annualSavingsLabel.getText().replace("$", "").replace(",", "");
                if (!savingsText.equals("(calculated)")) {
                    annualSavings = Double.parseDouble(savingsText);
                }
            } catch (Exception e) {
                // If parsing fails, use 0
            }
            
            // Update dashboard cards to show real-time preview
            double energyAmountForDashboard = energyAmount;
            if (energyAmountForDashboard == 0) {
                energyAmountForDashboard = 1; // Use a default value if energy amount is zero
            }
            updateDashboard(currentEmissions, targetEmissions, reductionPercentage, annualSavings, energyAmountForDashboard);
            
        } catch (Exception e) {
            System.err.println("Error calculating emissions: " + e.getMessage());
        }
    }
    
    /**
     * Calculate financial metrics for energy transition
     */
    private void calculateFinancialMetrics() {
        try {
            // Get selected energy sources
            String currentSourceName = (String) currentEnergySourceCombo.getSelectedItem();
            String newSourceName = (String) newEnergySourceCombo.getSelectedItem();
            
            if (currentSourceName == null || newSourceName == null) {
                return;
            }
            
            // Get the energy source IDs
            int currentSourceId = energySourceIds.get(currentSourceName);
            int newSourceId = energySourceIds.get(newSourceName);
            
            // Get energy amount in kWh
            double energyAmount = 0;
            try {
                energyAmount = Double.parseDouble(energyAmountField.getText());
            } catch (NumberFormatException e) {
                return;
            }
            
            // Convert kWh to kW assuming standard operating hours
            double capacityKW = energyAmount / (365 * 12); // Approximate capacity assuming 12 hours per day
            
            // Calculate implementation cost using predefined costs per kW
            double implementationCost = capacityKW * implementationCosts.get(newSourceId);
            
            // Get energy costs per kWh for both sources
            double currentCost = energyCosts.get(currentSourceId);
            double newCost = energyCosts.get(newSourceId);
            
            // Calculate annual operating costs
            double currentAnnualCost = currentCost * energyAmount;
            double newAnnualCost = newCost * energyAmount;
            
            // Calculate annual savings
            double annualSavings = currentAnnualCost - newAnnualCost;
            
            // Calculate payback period in months
            double paybackMonths = (annualSavings > 0) ? (implementationCost / annualSavings) * 12 : Double.POSITIVE_INFINITY;
            
            // Update the labels
            DecimalFormat df = new DecimalFormat("#,##0.00");
            implementationCostLabel.setText("$" + df.format(implementationCost));
            annualSavingsLabel.setText("$" + df.format(annualSavings));
            
            if (Double.isInfinite(paybackMonths)) {
                paybackPeriodLabel.setText("N/A (no cost savings)");
            } else {
                paybackPeriodLabel.setText(df.format(paybackMonths) + " months");
            }
            
            // Check if emissions have been calculated and update dashboard with current values
            try {
                double currentEmissions = Double.parseDouble(currentEmissionsField.getText().replace(",", ""));
                double targetEmissions = Double.parseDouble(targetEmissionsField.getText().replace(",", ""));
                double emissionsReduction = currentEmissions - targetEmissions;
                double reductionPercentage = (emissionsReduction / currentEmissions) * 100;
                
                // Update dashboard with real-time results
                updateDashboard(currentEmissions, targetEmissions, reductionPercentage, annualSavings, energyAmount);
            } catch (Exception e) {
                // If emissions values aren't available yet, just update what we can
                System.err.println("Could not update dashboard with emissions data: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error calculating financial metrics: " + e.getMessage());
        }
    }
    
    /**
     * Save and calculate with debugging to ensure recommendations are properly displayed
     */
    private void saveAndCalculate() {
        try {
            // Validate input
            if (businessNameField.getText().isEmpty() || 
                energyAmountField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields", 
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parse inputs
            String businessName = businessNameField.getText();
            double energyAmount;
            try {
                // Make sure to parse the energy amount correctly
                String energyAmountText = energyAmountField.getText().trim().replace(",", "");
                energyAmount = Double.parseDouble(energyAmountText);
                
                // Log the energy amount being saved
                System.out.println("Parsed energy amount: " + energyAmount);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid energy amount", 
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Get selected energy sources
            String currentSourceName = (String) currentEnergySourceCombo.getSelectedItem();
            String newSourceName = (String) newEnergySourceCombo.getSelectedItem();
            
            if (currentSourceName == null || newSourceName == null) {
                JOptionPane.showMessageDialog(this, "Please select both current and proposed energy sources", 
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int currentSourceId = energySourceIds.get(currentSourceName);
            int newSourceId = energySourceIds.get(newSourceName);
            
            // Log the energy sources being saved
            System.out.println("Current source: " + currentSourceName + " (ID: " + currentSourceId + ")");
            System.out.println("New source: " + newSourceName + " (ID: " + newSourceId + ")");
            
            // Get calculated values
            double currentEmissions = Double.parseDouble(currentEmissionsField.getText().replace(",", ""));
            double targetEmissions = Double.parseDouble(targetEmissionsField.getText().replace(",", ""));
            double emissionsReduction = currentEmissions - targetEmissions;
            
            // Extract financial values (remove $ and , characters)
            String implementationCostText = implementationCostLabel.getText().replace("$", "").replace(",", "");
            String annualSavingsText = annualSavingsLabel.getText().replace("$", "").replace(",", "");
            
            double implementationCost = Double.parseDouble(implementationCostText);
            double annualSavings = Double.parseDouble(annualSavingsText);
            
            // Extract payback period - convert to months if in years
            String paybackText = paybackPeriodLabel.getText();
            double paybackMonths;
            if (paybackText.contains("N/A")) {
                paybackMonths = Double.POSITIVE_INFINITY;
            } else {
                paybackMonths = Double.parseDouble(paybackText.split(" ")[0].replace(",", ""));
            }
            
            // Update dashboard with calculated results
            updateDashboard(currentEmissions, targetEmissions, 
                          (emissionsReduction / currentEmissions) * 100, annualSavings, energyAmount);
            
            // Ensure we have a valid businessId and userId
            if (businessId <= 0 && userAccount != null && userAccount.getBusinessId() > 0) {
                businessId = userAccount.getBusinessId();
            }
            
            // Save simulation record
            int simulationId = saveSimulationData(businessId, businessName, currentEmissions, 
                                                targetEmissions, annualSavings, paybackMonths);
            
            System.out.println("Saved simulation with ID: " + simulationId);
            
            // Save simulation detail for energy transition
            saveSimulationDetail(simulationId, currentSourceId, newSourceId, energyAmount, 
                              emissionsReduction, implementationCost, annualSavings);
            
            System.out.println("Energy amount saved: " + energyAmount);
            
            // Update comparison panel with the latest values
            updateComparisonPanel(currentSourceName, newSourceName, energyAmount);
            comparisonPanel.setVisible(true);
            
            // Update the details tab
            updateDetailsTab(
                businessName,
                currentSourceName,
                newSourceName, 
                currentEmissions,
                targetEmissions,
                (emissionsReduction / currentEmissions) * 100,
                annualSavings,
                energyAmount,
                implementationCost,
                paybackMonths
            );
            
            // Store the values for future reference in case of reload
            storeLastSavedValues(
                businessName,
                currentSourceName,
                newSourceName,
                currentEmissions,
                targetEmissions,
                energyAmount,
                implementationCost,
                annualSavings,
                paybackMonths
            );
            
            // Show success message
            JOptionPane.showMessageDialog(this, "Data saved and calculations performed successfully", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            
            // Switch to the Details tab to show results
            tabbedPane.setSelectedIndex(1);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values: " + e.getMessage(), 
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Add method to store last saved values in a more reliable way
    private void storeLastSavedValues(String businessName, String currentSource, String newSource,
                                 double currentEmissions, double targetEmissions, double energyAmount,
                                 double implementationCost, double annualSavings, double paybackMonths) {
        try {
            // Create a properties file in the user's home directory
            String userHome = System.getProperty("user.home");
            File configDir = new File(userHome, ".carbon_emissions");
            if (!configDir.exists()) {
                configDir.mkdir();
            }
            
            File configFile = new File(configDir, "last_simulation_" + businessId + ".properties");
            
            // Store values in properties
            java.util.Properties props = new java.util.Properties();
            props.setProperty("business_name", businessName);
            props.setProperty("current_source", currentSource);
            props.setProperty("new_source", newSource);
            props.setProperty("energy_amount", String.valueOf(energyAmount));
            props.setProperty("current_emissions", String.valueOf(currentEmissions));
            props.setProperty("target_emissions", String.valueOf(targetEmissions));
            props.setProperty("implementation_cost", String.valueOf(implementationCost));
            props.setProperty("annual_savings", String.valueOf(annualSavings));
            props.setProperty("payback_months", String.valueOf(paybackMonths));
            props.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
            
            // Save to file
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                props.store(out, "Last saved simulation values for business ID " + businessId);
                System.out.println("Saved configuration to: " + configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Error storing last saved values: " + e.getMessage());
            // Non-fatal error - continue without backup
        }
    }
    
    /**
     * Create a result card with formatted title, value and description
     */
    private JPanel createResultCard(String title, String value, String description, Color color) {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(color);
        
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        valuePanel.setBackground(Color.WHITE);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valuePanel.add(valueLabel);
        
        JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        descPanel.setBackground(Color.WHITE);
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setForeground(Color.GRAY);
        descPanel.add(descLabel);
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(valuePanel, BorderLayout.CENTER);
        mainPanel.add(descPanel, BorderLayout.SOUTH);
        
        return mainPanel;
    }
    
    /**
     * Save simulation data to database and return the simulation ID
     */
    private int saveSimulationData(int businessId, String businessName, double currentEmissions, 
                                 double targetEmissions, double annualSavings, double paybackMonths) throws SQLException {
        int simulationId = -1; // Initialize with invalid ID
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet generatedKeys = null;
        
        try {
            conn = CarbonEmissionsDBConnection.getConnection();
            
            // Set connection to not auto-commit
            conn.setAutoCommit(false);
            
            // Prepare the statement
            stmt = conn.prepareStatement(
                "INSERT INTO EmissionSimulation " +
                "(business_id, user_id, simulation_name, current_total_emissions, " + 
                "target_total_emissions, estimated_cost_savings, payback_period_months, simulation_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())", 
                PreparedStatement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, businessId);
            // Get user ID from session or use default 1 for demo
            int userId = (userAccount != null) ? userAccount.getUserId() : 1;
            stmt.setInt(2, userId);
            stmt.setString(3, businessName + " Simulation");
            stmt.setDouble(4, currentEmissions);
            stmt.setDouble(5, targetEmissions);
            stmt.setDouble(6, annualSavings);
            
            // Handle infinite payback period
            if (Double.isInfinite(paybackMonths)) {
                stmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(7, (int)Math.round(paybackMonths));
            }
            
            int affectedRows = stmt.executeUpdate();
            
            // Commit transaction
            conn.commit();
            
            if (affectedRows > 0) {
                generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    simulationId = generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            // Attempt to roll back the transaction
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            
            // Handle specific trigger error message
            if (e.getMessage().contains("trigger") || e.getMessage().contains("result set")) {
                System.err.println("Trigger error detected. Using fallback method to get simulation ID.");
                // Try to get the latest simulation ID for this business
                simulationId = getLatestSimulationId(businessId);
                System.err.println("Retrieved latest simulation ID: " + simulationId);
            } else {
                System.err.println("Error saving simulation data: " + e.getMessage());
                throw e; // Re-throw other database errors
            }
        } finally {
            // Clean up resources properly in reverse order
            if (generatedKeys != null) {
                try {
                    generatedKeys.close();
                } catch (SQLException e) {
                    System.err.println("Error closing result set: " + e.getMessage());
                }
            }
            
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    System.err.println("Error closing statement: " + e.getMessage());
                }
            }
            
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
        
        // If we still don't have a valid simulation ID, generate a unique one based on timestamp
        if (simulationId <= 0) {
            simulationId = (int)(System.currentTimeMillis() % 10000) + 1;
            System.err.println("Generated fallback simulation ID: " + simulationId);
        }
        
        return simulationId;
    }
    
    /**
     * Get the latest simulation ID for a business
     */
    private int getLatestSimulationId(int businessId) {
        int latestId = -1;
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT MAX(simulation_id) as max_id FROM EmissionSimulation " +
                 "WHERE business_id = ?")) {
                 
            stmt.setInt(1, businessId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                latestId = rs.getInt("max_id");
                // If no records found, MAX() returns 0, so add 1
                if (latestId == 0) {
                    latestId = 1;
                } else {
                    latestId++; // Use the next ID
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting latest simulation ID: " + e.getMessage());
            // Fallback to a timestamp-based ID
            latestId = (int)(System.currentTimeMillis() % 10000) + 1;
        }
        
        return latestId;
    }
    
    /**
     * Save simulation detail for energy transition
     */
    private void saveSimulationDetail(int simulationId, int currentSourceId, int newSourceId, 
                                    double energyAmount, double emissionsReduction, 
                                    double implementationCost, double annualSavings) throws SQLException {
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO SimulationDetail " +
                 "(simulation_id, current_source_id, new_source_id, energy_amount, " + 
                 "emissions_reduction, implementation_cost, annual_savings, incentive_amount) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            // Log the values being saved
            System.out.println("Saving simulation detail with:");
            System.out.println("- Simulation ID: " + simulationId);
            System.out.println("- Current source ID: " + currentSourceId);
            System.out.println("- New source ID: " + newSourceId);
            System.out.println("- Energy amount: " + energyAmount);
            
            stmt.setInt(1, simulationId);
            stmt.setInt(2, currentSourceId);
            stmt.setInt(3, newSourceId);
            stmt.setDouble(4, energyAmount);
            stmt.setDouble(5, emissionsReduction);
            stmt.setDouble(6, implementationCost);
            stmt.setDouble(7, annualSavings);
            
            // Calculate incentives using database function if possible
            double incentiveAmount = calculateIncentiveAmount(1, newSourceId, implementationCost);
            stmt.setDouble(8, incentiveAmount);
            
            stmt.executeUpdate();
            
            System.out.println("Successfully saved simulation detail");
        } catch (SQLException e) {
            System.err.println("Error saving simulation detail: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Calculate incentive amount for implementation cost, try using the database function
     */
    private double calculateIncentiveAmount(int countryId, int sourceId, double implementationCost) {
        double incentiveAmount = 0;
        
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             CallableStatement stmt = conn.prepareCall("{? = CALL calculate_available_incentives(?, ?, ?)}")) {
            
            stmt.registerOutParameter(1, java.sql.Types.DECIMAL);
            stmt.setInt(2, countryId);
            stmt.setInt(3, sourceId);
            stmt.setDouble(4, implementationCost);
            
            stmt.execute();
            
            incentiveAmount = stmt.getDouble(1);
            
        } catch (SQLException e) {
            System.err.println("Error calculating incentives: " + e.getMessage());
            
            // Fallback to a simple calculation if database function fails
            if (renewableFlags.getOrDefault(sourceId, false)) {
                // Simple incentive calculation for renewable sources: 15% of implementation cost
                incentiveAmount = implementationCost * 0.15;
            }
        }
        
        return incentiveAmount;
    }
    
    /**
     * Update the dashboard with calculated results
     */
    private void updateDashboard(double currentEmissions, double targetEmissions, 
                             double reductionPercentage, double annualSavings, double energyAmount) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat pctFormat = new DecimalFormat("#0.0");
        
        // Update Carbon Footprint card
        footprintValueLabel.setText(df.format(currentEmissions) + " tons CO2e");
        footprintDescLabel.setText("Target: " + df.format(targetEmissions) + " tons CO2e");
        
        // Update Energy Consumption card with actual energy amount
        energyValueLabel.setText(df.format(energyAmount) + " kWh");
        energyDescLabel.setText("Annual energy consumption");
        
        // Update Cost Savings card
        costValueLabel.setText("$" + df.format(annualSavings));
        costDescLabel.setText("Annual savings from transition");
        
        // Update Reduction Targets card
        targetValueLabel.setText(pctFormat.format(reductionPercentage) + "%");
        targetDescLabel.setText("Reduction from current emissions");
    }
    
    /**
     * Load existing business data from database if available
     */
    private void loadBusinessData() {
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM Business WHERE business_id = ?")) {
            
            stmt.setInt(1, businessId);
            System.out.println("Loading business data for business ID: " + businessId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String businessName = rs.getString("business_name");
                    businessNameField.setText(businessName);
                    
                    // Set business sector if it exists in the database
                    String industryType = rs.getString("industry_type");
                    if (industryType != null && !industryType.isEmpty()) {
                        businessSectorField.setText(industryType);
                    }
                    
                    System.out.println("Found business: " + businessName);
                    
                    // Load the latest simulation for this business and update UI
                    boolean dataLoaded = loadLatestSimulation(businessId);
                    
                    // If data was loaded, update UI components
                    if (dataLoaded && userAccount != null) {
                        System.out.println("Loaded user data for: " + userAccount.getFullName() + 
                                           " (Business ID: " + businessId + ")");
                                           
                        // Use SwingUtilities.invokeLater to ensure UI updates happen on EDT
                        SwingUtilities.invokeLater(() -> {
                            // Make sure dashboard tab shows loaded data
                            tabbedPane.setSelectedIndex(0);
                            // Give UI time to update
                            Timer timer = new Timer(500, e -> {
                                // Then switch to details tab
                                tabbedPane.setSelectedIndex(1);
                            });
                            timer.setRepeats(false);
                            timer.start();
                        });
                    }
                } else if (userAccount != null) {
                    // If no business found but we have a user account, show business creation form
                    System.out.println("No business found for user, showing creation form");
                    showBusinessCreationForm();
                }
            }
        } catch (SQLException e) {
            // More informative error message
            System.err.println("Error loading business data: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Could not load business data. Please contact administrator.\nError: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Show a form to create a new business
     */
    private void showBusinessCreationForm() {
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField nameField = new JTextField(20);
        JTextField industryField = new JTextField(20);
        JTextField addressField = new JTextField(20);
        JTextField cityField = new JTextField(20);
        JTextField stateField = new JTextField(20);
        JTextField postalCodeField = new JTextField(20);
        
        JComboBox<String> countryComboBox = new JComboBox<>();
        Map<String, Integer> countryMap = new HashMap<>();
        
        // Load countries from database
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT country_id, country_name FROM Country");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String countryName = rs.getString("country_name");
                int countryId = rs.getInt("country_id");
                countryComboBox.addItem(countryName);
                countryMap.put(countryName, countryId);
            }
            
            // Default to first country if exists
            if (countryComboBox.getItemCount() == 0) {
                // If no countries in database, add a default
                countryComboBox.addItem("United States");
                countryMap.put("United States", 1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading countries: " + e.getMessage());
            // Add a default country if loading fails
            countryComboBox.addItem("United States");
            countryMap.put("United States", 1);
        }
        
        formPanel.add(new JLabel("Business Name:*"));
        formPanel.add(nameField);
        formPanel.add(new JLabel("Industry Type:"));
        formPanel.add(industryField);
        formPanel.add(new JLabel("Street Address:"));
        formPanel.add(addressField);
        formPanel.add(new JLabel("City:"));
        formPanel.add(cityField);
        formPanel.add(new JLabel("State/Province:"));
        formPanel.add(stateField);
        formPanel.add(new JLabel("Postal Code:"));
        formPanel.add(postalCodeField);
        formPanel.add(new JLabel("Country:*"));
        formPanel.add(countryComboBox);
        
        int result = JOptionPane.showConfirmDialog(this, formPanel, 
                "Create New Business", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            String businessName = nameField.getText().trim();
            String industryType = industryField.getText().trim();
            String address = addressField.getText().trim();
            String city = cityField.getText().trim();
            String state = stateField.getText().trim();
            String postalCode = postalCodeField.getText().trim();
            String countryName = (String) countryComboBox.getSelectedItem();
            
            // Validate required fields
            if (businessName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Business name is required.", 
                        "Validation Error", JOptionPane.ERROR_MESSAGE);
                showBusinessCreationForm(); // Show form again
                return;
            }
            
            try {
                // Create new business
                int countryId = countryMap.get(countryName);
                int newBusinessId = userAccount.createBusiness(businessName, industryType, 
                        address, city, state, postalCode, countryId);
                
                if (newBusinessId > 0) {
                    businessId = newBusinessId;
                    businessNameField.setText(businessName);
                    businessSectorField.setText(industryType);
                    
                    JOptionPane.showMessageDialog(this, "Business created successfully!", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to create business.", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Load the latest simulation data from database
     * @return true if data was loaded successfully, false otherwise
     */
    private boolean loadLatestSimulation(int businessId) {
        try (Connection conn = CarbonEmissionsDBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT es.*, sd.* FROM EmissionSimulation es " +
                 "LEFT JOIN SimulationDetail sd ON es.simulation_id = sd.simulation_id " +
                 "WHERE es.business_id = ? " +
                 "ORDER BY es.simulation_date DESC, es.simulation_id DESC LIMIT 1")) {
            
            stmt.setInt(1, businessId);
            System.out.println("Loading latest simulation for business ID: " + businessId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int simulationId = rs.getInt("simulation_id");
                    System.out.println("Found simulation ID: " + simulationId);
                    
                    String simulationDate = rs.getString("simulation_date");
                    System.out.println("Simulation date: " + simulationDate);
                    
                    double currentEmissions = rs.getDouble("current_total_emissions");
                    double targetEmissions = rs.getDouble("target_total_emissions");
                    double annualSavings = rs.getDouble("estimated_cost_savings");
                    int paybackMonths = rs.getInt("payback_period_months");
                    
                    // Get simulation details
                    int currentSourceId = rs.getInt("current_source_id");
                    int newSourceId = rs.getInt("new_source_id");
                    double energyAmount = rs.getDouble("energy_amount");
                    double implementationCost = rs.getDouble("implementation_cost");
                    
                    System.out.println("Loaded simulation with:");
                    System.out.println("- Current source ID: " + currentSourceId);
                    System.out.println("- New source ID: " + newSourceId);
                    System.out.println("- Energy amount: " + energyAmount);
                    
                    // Ensure energy sources maps are loaded
                    if (energySourceIds.isEmpty()) {
                        loadEnergySources();
                    }
                    
                    // First update the energy amount field to ensure it happens before selections trigger calculations
                    // Temporarily remove document listeners to prevent recalculation
                    Document doc = energyAmountField.getDocument();
                    
                    // Store references to all document listeners on this document
                    for (DocumentListener listener : documentListeners) {
                        doc.removeDocumentListener(listener);
                    }
                    
                    // Format the energy amount with proper precision
                    DecimalFormat df = new DecimalFormat("#,##0.00");
                    energyAmountField.setText(String.valueOf(energyAmount));
                    
                    // Restore document listeners
                    for (DocumentListener listener : documentListeners) {
                        doc.addDocumentListener(listener);
                    }
                    
                    // Set energy sources in combo boxes - we need to prevent calculations until both are set
                    String currentSourceName = null;
                    String newSourceName = null;
                    
                    // First find the source names based on IDs
                    for (Map.Entry<Integer, String> entry : energySources.entrySet()) {
                        if (entry.getKey() == currentSourceId) {
                            currentSourceName = entry.getValue();
                        }
                        if (entry.getKey() == newSourceId) {
                            newSourceName = entry.getValue();
                        }
                    }
                    
                    System.out.println("Current source name: " + currentSourceName);
                    System.out.println("New source name: " + newSourceName);
                    
                    // Block listeners during updates
                    ActionListener[] currentListeners = currentEnergySourceCombo.getActionListeners();
                    ActionListener[] newListeners = newEnergySourceCombo.getActionListeners();
                    
                    // Remove all listeners temporarily
                    for (ActionListener listener : currentListeners) {
                        currentEnergySourceCombo.removeActionListener(listener);
                    }
                    for (ActionListener listener : newListeners) {
                        newEnergySourceCombo.removeActionListener(listener);
                    }
                    
                    // Set selections
                    boolean sourcesSet = false;
                    if (currentSourceName != null) {
                        for (int i = 0; i < currentEnergySourceCombo.getItemCount(); i++) {
                            if (currentEnergySourceCombo.getItemAt(i).equals(currentSourceName)) {
                                currentEnergySourceCombo.setSelectedIndex(i);
                                System.out.println("Set current source to: " + currentSourceName);
                                sourcesSet = true;
                                break;
                            }
                        }
                    }
                    
                    if (newSourceName != null) {
                        for (int i = 0; i < newEnergySourceCombo.getItemCount(); i++) {
                            if (newEnergySourceCombo.getItemAt(i).equals(newSourceName)) {
                                newEnergySourceCombo.setSelectedIndex(i);
                                System.out.println("Set new source to: " + newSourceName);
                                break;
                            }
                        }
                    }
                    
                    // Update input fields with previous simulation data
                    currentEmissionsField.setText(String.format("%.2f", currentEmissions));
                    targetEmissionsField.setText(String.format("%.2f", targetEmissions));
                    
                    // Format financial numbers with commas
                    implementationCostLabel.setText("$" + df.format(implementationCost));
                    annualSavingsLabel.setText("$" + df.format(annualSavings));
                    
                    // Format payback period
                    if (paybackMonths <= 0) {
                        paybackPeriodLabel.setText("N/A (no cost savings)");
                    } else {
                        paybackPeriodLabel.setText(df.format(paybackMonths) + " months");
                    }
                    
                    // Now we can restore listeners and force a recalculation
                    for (ActionListener listener : currentListeners) {
                        currentEnergySourceCombo.addActionListener(listener);
                    }
                    for (ActionListener listener : newListeners) {
                        newEnergySourceCombo.addActionListener(listener);
                    }
                    
                    // Force a recalculation to ensure all fields are updated
                    SwingUtilities.invokeLater(() -> {
                        System.out.println("Forcing calculation with:");
                        System.out.println("- Energy amount: " + energyAmountField.getText());
                        System.out.println("- Current source: " + currentEnergySourceCombo.getSelectedItem());
                        System.out.println("- New source: " + newEnergySourceCombo.getSelectedItem());
                        calculateValues();
                        System.out.println("Recalculated values after loading data");
                    });
                    
                    // Update dashboard with existing data
                    double reductionPercentage = (currentEmissions - targetEmissions) / currentEmissions * 100;
                    updateDashboard(currentEmissions, targetEmissions, reductionPercentage, annualSavings, energyAmount);
                    
                    // Update comparison panel with the latest values
                    if (currentSourceName != null && newSourceName != null) {
                        updateComparisonPanel(currentSourceName, newSourceName, energyAmount);
                        comparisonPanel.setVisible(true);
                    }
                    
                    // Update details tab
                    updateDetailsTab(
                        businessNameField.getText(),
                        currentSourceName,
                        newSourceName, 
                        currentEmissions,
                        targetEmissions,
                        reductionPercentage,
                        annualSavings,
                        energyAmount,
                        implementationCost,
                        paybackMonths
                    );
                    
                    // Remove initial message panel if it exists
                    if (initialMessagePanel.getParent() != null) {
                        Container parent = initialMessagePanel.getParent();
                        parent.remove(initialMessagePanel);
                        parent.add(detailsContentPanel, BorderLayout.CENTER);
                        parent.revalidate();
                        parent.repaint();
                    }
                    
                    System.out.println("Successfully loaded simulation data");
                    return true;
                } else {
                    System.out.println("No simulation data found for business ID: " + businessId + " - trying stored values");
                    if (loadStoredSimulationValues(businessId)) {
                        return true;
                    }
                    System.out.println("No stored simulation values found either");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading simulation data: " + e.getMessage());
            e.printStackTrace();
            
            // Try to load from stored values as fallback
            System.out.println("Trying to load from stored values due to database error");
            if (loadStoredSimulationValues(businessId)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Load values from previously stored properties file
     */
    private boolean loadStoredSimulationValues(int businessId) {
        try {
            // Try to load from properties file
            String userHome = System.getProperty("user.home");
            File configFile = new File(new File(userHome, ".carbon_emissions"), 
                                       "last_simulation_" + businessId + ".properties");
            
            if (!configFile.exists()) {
                return false;
            }
            
            System.out.println("Loading stored values from: " + configFile.getAbsolutePath());
            
            java.util.Properties props = new java.util.Properties();
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            }
            
            // Get values from properties
            String businessName = props.getProperty("business_name");
            String currentSource = props.getProperty("current_source");
            String newSource = props.getProperty("new_source");
            double energyAmount = Double.parseDouble(props.getProperty("energy_amount", "0"));
            double currentEmissions = Double.parseDouble(props.getProperty("current_emissions", "0"));
            double targetEmissions = Double.parseDouble(props.getProperty("target_emissions", "0"));
            double implementationCost = Double.parseDouble(props.getProperty("implementation_cost", "0"));
            double annualSavings = Double.parseDouble(props.getProperty("annual_savings", "0"));
            double paybackMonths = Double.parseDouble(props.getProperty("payback_months", "0"));
            
            System.out.println("Loaded stored values:");
            System.out.println("- Business name: " + businessName);
            System.out.println("- Current source: " + currentSource);
            System.out.println("- New source: " + newSource);
            System.out.println("- Energy amount: " + energyAmount);
            
            // Update UI with stored values, similar to loadLatestSimulation
            // Ensure energy sources maps are loaded
            if (energySourceIds.isEmpty()) {
                loadEnergySources();
            }
            
            // Set business name
            businessNameField.setText(businessName);
            
            // First update the energy amount field to ensure it happens before selections trigger calculations
            // Temporarily remove document listeners to prevent recalculation
            Document doc = energyAmountField.getDocument();
            
            // Store references to all document listeners on this document
            for (DocumentListener listener : documentListeners) {
                doc.removeDocumentListener(listener);
            }
            
            // Set the energy amount
            energyAmountField.setText(String.valueOf(energyAmount));
            
            // Restore document listeners
            for (DocumentListener listener : documentListeners) {
                doc.addDocumentListener(listener);
            }
            
            // Block listeners during combo box updates
            ActionListener[] currentListeners = currentEnergySourceCombo.getActionListeners();
            ActionListener[] newListeners = newEnergySourceCombo.getActionListeners();
            
            // Remove all listeners temporarily
            for (ActionListener listener : currentListeners) {
                currentEnergySourceCombo.removeActionListener(listener);
            }
            for (ActionListener listener : newListeners) {
                newEnergySourceCombo.removeActionListener(listener);
            }
            
            // Set energy source combo box values
            for (int i = 0; i < currentEnergySourceCombo.getItemCount(); i++) {
                if (currentEnergySourceCombo.getItemAt(i).equals(currentSource)) {
                    currentEnergySourceCombo.setSelectedIndex(i);
                    System.out.println("Set current source to: " + currentSource);
                    break;
                }
            }
            
            for (int i = 0; i < newEnergySourceCombo.getItemCount(); i++) {
                if (newEnergySourceCombo.getItemAt(i).equals(newSource)) {
                    newEnergySourceCombo.setSelectedIndex(i);
                    System.out.println("Set new source to: " + newSource);
                    break;
                }
            }
            
            // Update calculated fields
            DecimalFormat df = new DecimalFormat("#,##0.00");
            currentEmissionsField.setText(String.format("%.2f", currentEmissions));
            targetEmissionsField.setText(String.format("%.2f", targetEmissions));
            implementationCostLabel.setText("$" + df.format(implementationCost));
            annualSavingsLabel.setText("$" + df.format(annualSavings));
            
            // Format payback period
            if (paybackMonths <= 0 || Double.isInfinite(paybackMonths)) {
                paybackPeriodLabel.setText("N/A (no cost savings)");
            } else {
                paybackPeriodLabel.setText(df.format(paybackMonths) + " months");
            }
            
            // Restore listeners
            for (ActionListener listener : currentListeners) {
                currentEnergySourceCombo.addActionListener(listener);
            }
            for (ActionListener listener : newListeners) {
                newEnergySourceCombo.addActionListener(listener);
            }
            
            // Force a recalculation to ensure all fields are updated
            SwingUtilities.invokeLater(() -> {
                System.out.println("Forcing calculation with stored values:");
                System.out.println("- Energy amount: " + energyAmountField.getText());
                System.out.println("- Current source: " + currentEnergySourceCombo.getSelectedItem());
                System.out.println("- New source: " + newEnergySourceCombo.getSelectedItem());
                calculateValues();
                System.out.println("Recalculated values after loading stored data");
            });
            
            // Calculate emissions reduction percentage
            double emissionsReduction = currentEmissions - targetEmissions;
            double reductionPercentage = (currentEmissions > 0) ? 
                (emissionsReduction / currentEmissions) * 100 : 0;
            
            // Update dashboard with stored data
            updateDashboard(currentEmissions, targetEmissions, reductionPercentage, annualSavings, energyAmount);
            
            // Update comparison panel
            updateComparisonPanel(currentSource, newSource, energyAmount);
            comparisonPanel.setVisible(true);
            
            // Update details tab
            updateDetailsTab(
                businessName,
                currentSource,
                newSource, 
                currentEmissions,
                targetEmissions,
                reductionPercentage,
                annualSavings,
                energyAmount,
                implementationCost,
                paybackMonths
            );
            
            // Remove initial message panel if it exists
            if (initialMessagePanel.getParent() != null) {
                Container parent = initialMessagePanel.getParent();
                parent.remove(initialMessagePanel);
                parent.add(detailsContentPanel, BorderLayout.CENTER);
                parent.revalidate();
                parent.repaint();
            }
            
            System.out.println("Successfully loaded stored simulation values");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error loading stored simulation values: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        int response = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        
        if (response == JOptionPane.YES_OPTION) {
            // Clear session
            try {
                if (userAccount != null) {
                    userAccount.recordLogout();
                }
                com.carbon.emissions.auth.SessionManager.getInstance().clearSession();
            } catch (Exception e) {
                System.err.println("Error during logout: " + e.getMessage());
            }
            
            this.dispose();
            new MainLoginFrame().setVisible(true);
        }
    }
    
    /**
     * Create a panel for side-by-side comparison of energy sources
     */
    private JPanel createComparisonPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Energy Source Comparison",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 16)));
        
        // Create table to display comparison
        String[] columnNames = {"Metric", "Current Source", "Proposed Source", "Difference", "% Change"};
        Object[][] data = {
            {"Energy Source", "-", "-", "-", "-"},
            {"Cost per Unit ($)", "-", "-", "-", "-"},
            {"Annual Energy Cost ($)", "-", "-", "-", "-"},
            {"Emissions Factor (kg CO2/unit)", "-", "-", "-", "-"},
            {"Annual Emissions (tons CO2e)", "-", "-", "-", "-"}
        };
        
        JTable comparisonTable = new JTable(data, columnNames);
        comparisonTable.setRowHeight(25);
        comparisonTable.setFont(new Font("Arial", Font.PLAIN, 14));
        comparisonTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        comparisonTable.setEnabled(false);  // Make table non-editable
        
        // Customize table appearance
        comparisonTable.setShowGrid(true);
        comparisonTable.setGridColor(Color.LIGHT_GRAY);
        
        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(comparisonTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Update the comparison panel with energy source data
     */
    private void updateComparisonPanel(String currentSource, String newSource, double energyAmount) {
        if (!comparisonPanel.isVisible()) {
            comparisonPanel.setVisible(true);
        }
        
        // Get the energy source IDs
        int currentSourceId = energySourceIds.get(currentSource);
        int newSourceId = energySourceIds.get(newSource);
        
        // Get the costs and emission factors
        double currentCost = energyCosts.getOrDefault(currentSourceId, 0.0);
        double newCost = energyCosts.getOrDefault(newSourceId, 0.0);
        
        double currentFactor = carbonFactors.getOrDefault(currentSourceId, 0.0);
        double newFactor = carbonFactors.getOrDefault(newSourceId, 0.0);
        
        // Calculate annual costs and emissions
        double currentAnnualCost = currentCost * energyAmount;
        double newAnnualCost = newCost * energyAmount;
        
        double currentEmissions = currentFactor * energyAmount / 1000; // Convert kg to tons
        double newEmissions = newFactor * energyAmount / 1000; // Convert kg to tons
        
        // Calculate differences
        double costDiff = newAnnualCost - currentAnnualCost;
        double emissionsDiff = newEmissions - currentEmissions;
        
        double costPctChange = (currentAnnualCost == 0) ? 0 : (costDiff / currentAnnualCost) * 100;
        double emissionsPctChange = (currentEmissions == 0) ? 0 : (emissionsDiff / currentEmissions) * 100;
        double factorPctChange = (currentFactor == 0) ? 0 : ((newFactor - currentFactor) / currentFactor) * 100;
        
        // Format values
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat pctFormat = new DecimalFormat("+#0.0;-#0.0");
        
        // Get the comparison table from the panel
        JScrollPane scrollPane = (JScrollPane) comparisonPanel.getComponent(0);
        JTable comparisonTable = (JTable) scrollPane.getViewport().getView();
        
        // Update table data
        comparisonTable.setValueAt(currentSource, 0, 1);
        comparisonTable.setValueAt(newSource, 0, 2);
        comparisonTable.setValueAt("→", 0, 3);
        comparisonTable.setValueAt("", 0, 4);
        
        comparisonTable.setValueAt(df.format(currentCost), 1, 1);
        comparisonTable.setValueAt(df.format(newCost), 1, 2);
        comparisonTable.setValueAt(df.format(newCost - currentCost), 1, 3);
        comparisonTable.setValueAt(pctFormat.format((newCost - currentCost) / currentCost * 100) + "%", 1, 4);
        
        comparisonTable.setValueAt(df.format(currentAnnualCost), 2, 1);
        comparisonTable.setValueAt(df.format(newAnnualCost), 2, 2);
        comparisonTable.setValueAt(df.format(costDiff), 2, 3);
        comparisonTable.setValueAt(pctFormat.format(costPctChange) + "%", 2, 4);
        
        comparisonTable.setValueAt(df.format(currentFactor), 3, 1);
        comparisonTable.setValueAt(df.format(newFactor), 3, 2);
        comparisonTable.setValueAt(df.format(newFactor - currentFactor), 3, 3);
        comparisonTable.setValueAt(pctFormat.format(factorPctChange) + "%", 3, 4);
        
        comparisonTable.setValueAt(df.format(currentEmissions), 4, 1);
        comparisonTable.setValueAt(df.format(newEmissions), 4, 2);
        comparisonTable.setValueAt(df.format(emissionsDiff), 4, 3);
        comparisonTable.setValueAt(pctFormat.format(emissionsPctChange) + "%", 4, 4);
        
        // Color code the cells based on whether the change is positive or negative
        // For costs, negative (cost reduction) is good
        // For emissions, negative (emissions reduction) is good
        colorCodeTable(comparisonTable);
    }
    
    /**
     * Color code table cells based on whether changes are positive or negative
     */
    private void colorCodeTable(JTable table) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                
                if (column == 3 || column == 4) { // Difference or % change columns
                    if (row >= 1) { // Skip the Energy Source row
                        String text = value.toString();
                        boolean isNegative = text.contains("-");
                        
                        // For costs (rows 1-2), negative is good
                        // For emissions (rows 3-4), negative is good
                        boolean isGood = isNegative;
                        
                        if (isGood) {
                            c.setForeground(new Color(0, 128, 0)); // Green for good
                        } else if (!text.equals("") && !text.equals("0.00") && !text.equals("+0.0%") && !text.equals("-0.0%")) {
                            c.setForeground(new Color(192, 0, 0)); // Red for bad
                        } else {
                            c.setForeground(Color.BLACK); // Black for neutral
                        }
                    }
                } else {
                    c.setForeground(Color.BLACK); // Default color
                }
                
                // Center align all cells
                ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                
                return c;
            }
        });
    }
    
    /**
     * Update the details tab with calculation results
     */
    private void updateDetailsTab(String businessName, String currentSource, String newSource, 
                               double currentEmissions, double targetEmissions, 
                               double reductionPercentage, double annualSavings,
                               double energyAmount, double implementationCost, 
                               double paybackMonths) {
        
        // Remove the initial message if it's showing
        if (initialMessagePanel.getParent() != null) {
            Container parent = initialMessagePanel.getParent();
            parent.remove(initialMessagePanel);
            parent.add(detailsContentPanel, BorderLayout.CENTER);
            parent.revalidate();
            parent.repaint();
        }
        
        // Update summary info
        businessNameValueLabel.setText(businessName);
        transitionValueLabel.setText(currentSource + " → " + newSource);
        
        // Update metrics table
        DecimalFormat df = new DecimalFormat("#,##0.00");
        DecimalFormat pctFormat = new DecimalFormat("+#0.0;-#0.0");
        
        // Row 0: Energy Source
        metricsTable.setValueAt(currentSource, 0, 1);
        metricsTable.setValueAt(newSource, 0, 2);
        metricsTable.setValueAt("→", 0, 3);
        metricsTable.setValueAt("", 0, 4);
        
        // Row 1: Implementation Cost
        metricsTable.setValueAt("N/A", 1, 1);
        metricsTable.setValueAt("$" + df.format(implementationCost), 1, 2);
        metricsTable.setValueAt("$" + df.format(implementationCost), 1, 3);
        metricsTable.setValueAt("N/A", 1, 4);
        
        // Get energy costs from the data maps
        int currentSourceId = energySourceIds.get(currentSource);
        int newSourceId = energySourceIds.get(newSource);
        double currentCost = energyCosts.get(currentSourceId);
        double newCost = energyCosts.get(newSourceId);
        
        // Calculate annual costs
        double currentAnnualCost = currentCost * energyAmount;
        double newAnnualCost = newCost * energyAmount;
        
        // Row 2: Annual Energy Cost
        metricsTable.setValueAt("$" + df.format(currentAnnualCost), 2, 1);
        metricsTable.setValueAt("$" + df.format(newAnnualCost), 2, 2);
        metricsTable.setValueAt("$" + df.format(newAnnualCost - currentAnnualCost), 2, 3);
        metricsTable.setValueAt(pctFormat.format(((newAnnualCost - currentAnnualCost) / currentAnnualCost) * 100) + "%", 2, 4);
        
        // Row 3: Annual Savings
        metricsTable.setValueAt("$0.00", 3, 1);
        metricsTable.setValueAt("$" + df.format(annualSavings), 3, 2);
        metricsTable.setValueAt("$" + df.format(annualSavings), 3, 3);
        metricsTable.setValueAt("N/A", 3, 4);
        
        // Row 4: Payback Period
        metricsTable.setValueAt("N/A", 4, 1);
        if (Double.isInfinite(paybackMonths)) {
            metricsTable.setValueAt("N/A", 4, 2);
            metricsTable.setValueAt("N/A", 4, 3);
        } else {
            metricsTable.setValueAt(df.format(paybackMonths) + " months", 4, 2);
            metricsTable.setValueAt(df.format(paybackMonths) + " months", 4, 3);
        }
        metricsTable.setValueAt("N/A", 4, 4);
        
        // Row 5: Carbon Emissions
        metricsTable.setValueAt(df.format(currentEmissions), 5, 1);
        metricsTable.setValueAt(df.format(targetEmissions), 5, 2);
        metricsTable.setValueAt(df.format(targetEmissions - currentEmissions), 5, 3);
        metricsTable.setValueAt(pctFormat.format(((targetEmissions - currentEmissions) / currentEmissions) * 100) + "%", 5, 4);
        
        // Row 6: Emissions Reduction
        metricsTable.setValueAt("0%", 6, 1);
        metricsTable.setValueAt(df.format(reductionPercentage) + "%", 6, 2);
        metricsTable.setValueAt(df.format(reductionPercentage) + "%", 6, 3);
        metricsTable.setValueAt("N/A", 6, 4);
        
        // Color code the cells
        colorCodeTable(metricsTable);
    }
    
    // Add a new method to ensure the dashboard fields are properly populated
    private void ensureDashboardFieldsInitialized() {
        // Check if the energy amount field is empty or zero 
        // but we have a valid current source and new source selected
        if (energyAmountField != null && 
            currentEnergySourceCombo != null && 
            newEnergySourceCombo != null &&
            currentEnergySourceCombo.getSelectedItem() != null &&
            newEnergySourceCombo.getSelectedItem() != null) {
            
            double energyAmount = 0;
            try {
                if (!energyAmountField.getText().isEmpty()) {
                    energyAmount = Double.parseDouble(energyAmountField.getText().trim());
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
            
            if (energyAmount == 0) {
                System.out.println("Dashboard fields need initialization - forcing calculation");
                calculateValues();
            }
        }
    }

    /**
     * Filter energy sources to exclude Nuclear, Geothermal and Tidal
     */
    private void filterEnergySourcesForDropdown() {
        // Remove Nuclear, Geothermal, and Tidal from the dropdown lists
        Map<Integer, String> filteredSources = new HashMap<>();
        Map<String, Integer> filteredIds = new HashMap<>();
        
        for (Map.Entry<Integer, String> entry : energySources.entrySet()) {
            String source = entry.getValue();
            if (!source.equals("Nuclear") && !source.equals("Geothermal") && !source.equals("Tidal")) {
                filteredSources.put(entry.getKey(), source);
                filteredIds.put(source, entry.getKey());
            }
        }
        
        // Update the combo boxes with filtered values
        currentEnergySourceCombo.removeAllItems();
        newEnergySourceCombo.removeAllItems();
        
        for (String source : filteredSources.values()) {
            currentEnergySourceCombo.addItem(source);
            newEnergySourceCombo.addItem(source);
        }
    }
} 