package com.carbon.emissions.ui;

import com.carbon.emissions.CarbonEmissionsDataAccess;
import com.carbon.emissions.CarbonEmissionsDBConnection;
import com.carbon.emissions.auth.SessionManager;
import com.carbon.emissions.auth.UserAccount;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

/**
 * Analysis Dashboard to provide an overview of business emissions data
 */
public class AnalysisDashboard extends JFrame {
    
    private String username;
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    
    // Data access components
    private CarbonEmissionsDataAccess dataAccess;
    private UserAccount currentUser;
    
    // Dashboard components
    private JLabel totalBusinessesLabel;
    private JLabel avgEmissionsLabel;
    private JLabel renewablePercentageLabel;
    
    // Details components
    private DefaultTableModel businessTableModel;
    private JTable businessTable;
    
    public AnalysisDashboard(String username) {
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
        setTitle("Analysis Dashboard - Carbon Emissions Management");
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
        tabbedPane.addTab("Details", createDetailsPanel());
        tabbedPane.addTab("Visualization", createVisualizationPanel());
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Load initial data
        loadDashboardData();
        loadBusinessData();
    }
    
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(39, 174, 96)); // Brighter green
        panel.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        
        JLabel welcomeLabel = new JLabel("Analysis Dashboard - " + username);
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
        
        // Business count card
        totalBusinessesLabel = new JLabel("Loading...");
        totalBusinessesLabel.setFont(new Font("Arial", Font.BOLD, 36));
        totalBusinessesLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(createSummaryCard(
                "Total Businesses", 
                totalBusinessesLabel, 
                "Total number of businesses in the system",
                new Color(46, 139, 87))); // Sea green
        
        // Average emissions card
        avgEmissionsLabel = new JLabel("Loading...");
        avgEmissionsLabel.setFont(new Font("Arial", Font.BOLD, 36));
        avgEmissionsLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(createSummaryCard(
                "Average Emissions", 
                avgEmissionsLabel, 
                "Average CO₂ emissions across all businesses",
                new Color(70, 130, 180))); // Steel blue
        
        // Renewable energy percentage card
        renewablePercentageLabel = new JLabel("Loading...");
        renewablePercentageLabel.setFont(new Font("Arial", Font.BOLD, 36));
        renewablePercentageLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(createSummaryCard(
                "Renewable Energy", 
                renewablePercentageLabel, 
                "Percentage of businesses using renewable energy",
                new Color(218, 165, 32))); // Goldenrod
        
        // Top industry card - Using a JLabel instead of a string
        JLabel topIndustryLabel = new JLabel("Technology");
        topIndustryLabel.setFont(new Font("Arial", Font.BOLD, 36));
        topIndustryLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(createSummaryCard(
                "Top Industry", 
                topIndustryLabel, 
                "Industry with highest emission reduction potential",
                new Color(178, 34, 34))); // Firebrick
        
        return panel;
    }
    
    private JPanel createSummaryCard(String title, String value, String description, Color color) {
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 36));
        valueLabel.setHorizontalAlignment(JLabel.CENTER);
        return createSummaryCard(title, valueLabel, description, color);
    }
    
    private JPanel createSummaryCard(String title, JLabel valueLabel, String description, Color color) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(color);
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setForeground(Color.GRAY);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        panel.add(descLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create column names and table model
        String[] columnNames = {"Business ID", "Business Name", "Industry", "Current Emissions", 
                                "Target Emissions", "Energy Source", "Renewable", "Cost Savings"};
        businessTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        businessTable = new JTable(businessTableModel);
        businessTable.setRowHeight(25);
        businessTable.setFont(new Font("Arial", Font.PLAIN, 14));
        businessTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        
        // Color code the cells based on renewable energy status
        businessTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    // Get renewable status (column 6)
                    Object renewableVal = table.getValueAt(row, 6);
                    boolean isRenewable = renewableVal != null && renewableVal.toString().equalsIgnoreCase("Yes");
                    
                    if (isRenewable) {
                        c.setBackground(new Color(240, 255, 240)); // Light green
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                
                return c;
            }
        });
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(businessTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Add components to panel
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createVisualizationPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create title for the chart
        JLabel titleLabel = new JLabel("Carbon Emissions by Business Sector");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Create the dataset for the pie chart
        DefaultPieDataset dataset = new DefaultPieDataset();
        
        // Add data to the chart
        try {
            Connection conn = CarbonEmissionsDBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT IFNULL(b.industry_type, 'Unknown') as industry_type, " +
                "SUM(IFNULL(es.current_total_emissions, 0)) as total_emissions " +
                "FROM Business b " +
                "LEFT JOIN EmissionSimulation es ON b.business_id = es.business_id " +
                "GROUP BY industry_type " +
                "ORDER BY total_emissions DESC");
            
            ResultSet rs = stmt.executeQuery();
            
            boolean hasData = false;
            
            while (rs.next()) {
                String industry = rs.getString("industry_type");
                double emissions = rs.getDouble("total_emissions");
                
                // Skip null or empty industries
                if (industry == null || industry.isEmpty()) {
                    industry = "Unknown";
                }
                
                // Only add non-zero data
                if (emissions > 0) {
                    dataset.setValue(industry, emissions);
                    hasData = true;
                }
            }
            
            // If no data with emissions > 0 was found, add sample data
            if (!hasData) {
                dataset.setValue("Technology", 150000);
                dataset.setValue("Manufacturing", 320000);
                dataset.setValue("Healthcare", 90000);
                dataset.setValue("Retail", 70000);
                dataset.setValue("Energy", 280000);
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (SQLException e) {
            // If there's an error, use sample data
            dataset.setValue("Technology", 150000);
            dataset.setValue("Manufacturing", 320000);
            dataset.setValue("Healthcare", 90000);
            dataset.setValue("Retail", 70000);
            dataset.setValue("Energy", 280000);
            
            System.err.println("Error loading emission data: " + e.getMessage());
        }
        
        // Create the chart
        JFreeChart chart = ChartFactory.createPieChart(
                null,  // chart title
                dataset,
                true,  // include legend
                true,
                false);
        
        // Customize chart appearance
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionOutlinesVisible(false);
        plot.setLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.setBackgroundPaint(new Color(240, 248, 255));
        plot.setOutlineVisible(false);
        
        // Create the chart panel
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(500, 400));
        chartPanel.setBackground(new Color(240, 248, 255));
        
        // Add the chart to the panel
        panel.add(chartPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void loadDashboardData() {
        try {
            // Get total number of businesses
            Connection conn = CarbonEmissionsDBConnection.getConnection();
            
            // Query for total businesses
            PreparedStatement businessStmt = conn.prepareStatement(
                "SELECT COUNT(*) as count FROM Business");
            ResultSet businessRs = businessStmt.executeQuery();
            
            if (businessRs.next()) {
                int count = businessRs.getInt("count");
                totalBusinessesLabel.setText(String.valueOf(count));
            } else {
                totalBusinessesLabel.setText("0");
            }
            
            businessRs.close();
            businessStmt.close();
            
            // Query for average emissions
            PreparedStatement emissionsStmt = conn.prepareStatement(
                "SELECT AVG(IFNULL(current_total_emissions, 0)) as avg_emissions " +
                "FROM EmissionSimulation");
            ResultSet emissionsRs = emissionsStmt.executeQuery();
            
            if (emissionsRs.next()) {
                double avgEmissions = emissionsRs.getDouble("avg_emissions");
                DecimalFormat df = new DecimalFormat("#,###");
                avgEmissionsLabel.setText(df.format(avgEmissions) + " tons CO₂");
            } else {
                avgEmissionsLabel.setText("0 tons CO₂");
            }
            
            emissionsRs.close();
            emissionsStmt.close();
            
            // Query for renewable energy percentage
            PreparedStatement renewableStmt = conn.prepareStatement(
                "SELECT " +
                "  COUNT(DISTINCT bes.business_id) as renewable_count, " +
                "  (SELECT COUNT(*) FROM Business) as total_count " +
                "FROM BusinessEnergySource bes " +
                "JOIN EnergySource es ON bes.source_id = es.source_id " +
                "WHERE es.is_renewable = TRUE");
            
            ResultSet renewableRs = renewableStmt.executeQuery();
            
            if (renewableRs.next()) {
                int renewableCount = renewableRs.getInt("renewable_count");
                int totalCount = renewableRs.getInt("total_count");
                
                if (totalCount > 0) {
                    double percentage = (double) renewableCount / totalCount * 100;
                    DecimalFormat df = new DecimalFormat("#.#");
                    renewablePercentageLabel.setText(df.format(percentage) + "%");
                } else {
                    renewablePercentageLabel.setText("0%");
                }
            } else {
                renewablePercentageLabel.setText("0%");
            }
            
            renewableRs.close();
            renewableStmt.close();
            
            conn.close();
            
        } catch (SQLException e) {
            // Show default values if there's an error
            totalBusinessesLabel.setText("0");
            avgEmissionsLabel.setText("0 tons CO₂");
            renewablePercentageLabel.setText("0%");
            
            System.err.println("Error loading dashboard data: " + e.getMessage());
        }
    }
    
    private void loadBusinessData() {
        try {
            // Clear existing data
            businessTableModel.setRowCount(0);
            
            // Connect to database
            Connection conn = CarbonEmissionsDBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT b.business_id, b.business_name, b.industry_type, " +
                "MAX(IFNULL(es.current_total_emissions, 0)) as current_total_emissions, " +
                "MAX(IFNULL(es.target_total_emissions, 0)) as target_total_emissions, " +
                "MAX(ens.source_name) as source_name, " +
                "MAX(IFNULL(ens.is_renewable, 0)) as is_renewable, " +
                "MAX(IFNULL(es.estimated_cost_savings, 0)) as estimated_cost_savings " +
                "FROM Business b " +
                "LEFT JOIN EmissionSimulation es ON b.business_id = es.business_id " +
                "LEFT JOIN BusinessEnergySource bes ON b.business_id = bes.business_id " +
                "LEFT JOIN EnergySource ens ON bes.source_id = ens.source_id " +
                "GROUP BY b.business_id, b.business_name, b.industry_type " +
                "ORDER BY b.business_name");
            
            ResultSet rs = stmt.executeQuery();
            
            DecimalFormat df = new DecimalFormat("#,###.##");
            
            // Add a placeholder row if no data exists
            if (!rs.isBeforeFirst()) {
                businessTableModel.addRow(new Object[] {
                    0,
                    "No business data available",
                    "N/A",
                    "0",
                    "0",
                    "N/A",
                    "No",
                    "$0"
                });
            } else {
                while (rs.next()) {
                    int businessId = rs.getInt("business_id");
                    String businessName = rs.getString("business_name");
                    String industry = rs.getString("industry_type");
                    double currentEmissions = rs.getDouble("current_total_emissions");
                    double targetEmissions = rs.getDouble("target_total_emissions");
                    String sourceName = rs.getString("source_name");
                    boolean isRenewable = rs.getBoolean("is_renewable");
                    double costSavings = rs.getDouble("estimated_cost_savings");
                    
                    // Replace null values with placeholder text
                    if (industry == null || industry.isEmpty()) industry = "Not specified";
                    if (sourceName == null || sourceName.isEmpty()) sourceName = "Not specified";
                    
                    // Add row to table
                    businessTableModel.addRow(new Object[] {
                        businessId,
                        businessName,
                        industry,
                        df.format(currentEmissions),
                        df.format(targetEmissions),
                        sourceName,
                        isRenewable ? "Yes" : "No",
                        "$" + df.format(costSavings)
                    });
                }
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
        } catch (SQLException e) {
            System.err.println("Error loading business data: " + e.getMessage());
            
            // Add placeholder data if there's an error
            businessTableModel.addRow(new Object[] {
                0,
                "Database connection error",
                "Error",
                "0",
                "0",
                "Error",
                "No",
                "$0"
            });
            
            JOptionPane.showMessageDialog(this, 
                    "Error loading business data: " + e.getMessage(),
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
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
        try {
            SessionManager.getInstance().logout();
        } catch (SQLException e) {
            System.err.println("Error during logout: " + e.getMessage());
        }
        
        // Close this window
        dispose();
        
        // Open login window
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainLoginFrame loginFrame = new MainLoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                AnalysisDashboard dashboard = new AnalysisDashboard("analyst");
                dashboard.setVisible(true);
            }
        });
    }
} 