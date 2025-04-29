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
        for (int i = 0; i < 5; i++) {
            orderTable.getColumnModel().getColumn(i).setCellRenderer(new CenterRenderer());
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
        dialog.setTitle("Order #" + order.getId() + " Details");
        dialog.setModal(true);
        dialog.setSize(500, 400);
        dialog.setLayout(new BorderLayout(10, 10));
        
        // Order info
        JPanel infoPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        infoPanel.add(new JLabel("Order ID:"));
        infoPanel.add(new JLabel(String.valueOf(order.getId())));
        infoPanel.add(new JLabel("Status:"));
        
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"pending", "completed", "cancelled"});
        statusCombo.setSelectedItem(order.getStatus());
        infoPanel.add(statusCombo);
        
        infoPanel.add(new JLabel("Total:"));
        infoPanel.add(new JLabel(String.format("$%.2f", order.getTotal())));
        infoPanel.add(new JLabel("Created At:"));
        infoPanel.add(new JLabel(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        
        // Items table
        String[] columns = {"Item", "Qty", "Price"};
        DefaultTableModel itemsModel = new DefaultTableModel(columns, 0);
        JTable itemsTable = new JTable(itemsModel);
        
        for (OrderItem item : order.getItems()) {
            itemsModel.addRow(new Object[] {
                item.getMenuItem().getName(),
                item.getQuantity(),
                String.format("$%.2f", item.getPriceAtOrder() * item.getQuantity())
            });
        }
        
        // Buttons
        JPanel buttonPanel = new JPanel();
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
        
        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
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
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
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
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value,
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
    
    // Center alignment renderer for the table
    class CenterRenderer extends DefaultTableCellRenderer {
        public CenterRenderer() {
            setHorizontalAlignment(JLabel.CENTER); // Align text to center
        }
    }
}
