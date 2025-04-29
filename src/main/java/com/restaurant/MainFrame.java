package com.restaurant;

import javax.swing.*;

public class MainFrame extends JFrame {
    public MainFrame() {
        setTitle("Restaurant POS - Admin System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Menu Management", new MenuManagementPanel());
        tabbedPane.addTab("Order Management", new OrderManagementPanel());
        tabbedPane.addTab("Reports", new ReportPanel());
        
        add(tabbedPane);
    }
}