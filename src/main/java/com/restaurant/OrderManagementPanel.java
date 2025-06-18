package com.restaurant;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderManagementPanel extends JPanel {
    private final OrderDAO orderDAO = new OrderDAO();
    private final DefaultTableModel tableModel;
    private final JTable orderTable;
    private final JComboBox<String> statusFilter;

    public OrderManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table setup
        String[] columns = {"Order ID", "Total", "Status", "Created At", "Items", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only Action column is editable
            }
        };
        
        orderTable = new JTable(tableModel);
        orderTable.setRowHeight(30);
        orderTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        orderTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Center alignment for all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < 5; i++) {
            orderTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Filter by Status:"));
        
        statusFilter = new JComboBox<>(new String[]{"All", "pending", "completed", "cancelled"});
        statusFilter.addActionListener(e -> refreshOrderData());
        filterPanel.add(statusFilter);
        
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshOrderData());
        filterPanel.add(refreshBtn);
        
        // Add components
        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(orderTable), BorderLayout.CENTER);
        
        refreshOrderData();
    }
    
    private void refreshOrderData() {
        tableModel.setRowCount(0);
        String selectedStatus = (String) statusFilter.getSelectedItem();
        List<Order> orders;
        
        if ("All".equals(selectedStatus)) {
            orders = orderDAO.getAllOrders();
        } else {
            orders = orderDAO.getOrdersByStatus(selectedStatus);
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        for (Order order : orders) {
            StringBuilder items = new StringBuilder();
            for (OrderItem item : order.getItems()) {
                items.append(item.getMenuItem().getName())
                    .append(" (x").append(item.getQuantity()).append("), ");
            }
            if (items.length() > 0) {
                items.setLength(items.length() - 2); // Remove last comma
            }
            
            tableModel.addRow(new Object[]{
                order.getId(),
                String.format("$%.2f", order.getTotal()),
                capitalize(order.getStatus()),
                order.getCreatedAt().format(formatter),
                items.toString(),
                "View/Update"
            });
        }
    }
    
    private void showOrderDetails(Order order) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Order Details #" + order.getId());
        dialog.setModal(true);
        dialog.setSize(500, 400);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Vertical order info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Order Information"));
        
        // Order ID
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        idPanel.add(new JLabel("Order ID:"));
        idPanel.add(new JLabel(String.valueOf(order.getId())));
        infoPanel.add(idPanel);
        
        // Add vertical spacing
        infoPanel.add(Box.createVerticalStrut(10));
        
        // Status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.add(new JLabel("Status:"));
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"pending", "completed", "cancelled"});
        statusCombo.setSelectedItem(order.getStatus());
        statusPanel.add(statusCombo);
        infoPanel.add(statusPanel);
        
        // Add vertical spacing
        infoPanel.add(Box.createVerticalStrut(10));
        
        // Created At
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        datePanel.add(new JLabel("Created At:"));
        datePanel.add(new JLabel(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        infoPanel.add(datePanel);
        
        // Add vertical spacing
        infoPanel.add(Box.createVerticalStrut(10));
        
        // Total
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        totalPanel.add(new JLabel("Total:"));
        totalPanel.add(new JLabel(String.format("$%.2f", order.getTotal())));
        infoPanel.add(totalPanel);
        
        // Items table
        String[] columns = {"Item", "Qty", "Price"};
        DefaultTableModel itemsModel = new DefaultTableModel(columns, 0);
        JTable itemsTable = new JTable(itemsModel);
        itemsTable.setRowHeight(25);
        
        // Center align all columns in items table
        DefaultTableCellRenderer itemsCenterRenderer = new DefaultTableCellRenderer();
        itemsCenterRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < itemsTable.getColumnCount(); i++) {
            itemsTable.getColumnModel().getColumn(i).setCellRenderer(itemsCenterRenderer);
        }
        
        for (OrderItem item : order.getItems()) {
            itemsModel.addRow(new Object[] {
                item.getMenuItem().getName(),
                item.getQuantity(),
                String.format("$%.2f", item.getPriceAtOrder() * item.getQuantity())
            });
        }
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Changes");
        saveBtn.addActionListener(e -> {
            order.setStatus((String) statusCombo.getSelectedItem());
            if (orderDAO.saveOrder(order)) {
                JOptionPane.showMessageDialog(dialog, "Order updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshOrderData();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to update order", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelBtn = new JButton("Close");
        cancelBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        // Add components to main panel
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    // Button Renderer
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    // Button Editor
    class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int editingRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            editingRow = row;
            button.setText((value == null) ? "" : value.toString());
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            int orderId = (int) tableModel.getValueAt(editingRow, 0);
            Order order = orderDAO.getOrderById(orderId);
            
            if (button.getText().equals("View/Update")) {
                showOrderDetails(order);
            }
            
            return button.getText();
        }
    }
}