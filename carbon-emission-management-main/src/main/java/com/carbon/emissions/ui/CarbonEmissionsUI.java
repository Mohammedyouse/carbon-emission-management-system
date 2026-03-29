package com.carbon.emissions.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Main class to launch the Carbon Emissions Management System UI
 */
public class CarbonEmissionsUI {
    
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Custom button styling
            UIManager.put("Button.background", new Color(39, 174, 96)); // Brighter green
            UIManager.put("Button.foreground", Color.BLACK);
            UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 14));
            UIManager.put("Button.margin", new Insets(8, 14, 8, 14));
            UIManager.put("Button.focusPainted", false);
            
            // Custom panel styling
            UIManager.put("Panel.background", new Color(245, 250, 255)); // Lighter blue
            
            // Tab styling
            UIManager.put("TabbedPane.selected", new Color(225, 245, 235)); // Light mint green
            UIManager.put("TabbedPane.background", new Color(245, 250, 255));
            UIManager.put("TabbedPane.contentAreaColor", new Color(245, 250, 255));
            UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.PLAIN, 14));
            
            // Table styling for analysis dashboard
            UIManager.put("Table.gridColor", new Color(220, 230, 230));
            UIManager.put("Table.selectionBackground", new Color(200, 230, 220));
            UIManager.put("Table.selectionForeground", Color.BLACK);
            UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));
            
            // Label and text field styling
            UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 14));
            
            // Border styling
            UIManager.put("TitledBorder.font", new Font("Segoe UI", Font.BOLD, 14));
            UIManager.put("TitledBorder.titleColor", new Color(39, 174, 96));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Launch the login frame
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainLoginFrame loginFrame = new MainLoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }
} 