package com.restaurant;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;

public class App 
{
    public static void main(String[] args) {
        // Initialize database connection
        KoneksiDB.getSessionFactory();
        
        // Setup UI
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            KoneksiDB.shutdown();
        }));
    }
}
