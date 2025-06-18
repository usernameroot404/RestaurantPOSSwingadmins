package com.restaurant;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
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
        String[] columns = {
            "ID", 
            "Total", 
            "Status", 
            "Tipe Order", 
            "Pembayaran", 
            "Tanggal", 
            "Aksi"
        };
        
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only Action column is editable
            }
        };
        
        orderTable = new JTable(tableModel);
        orderTable.setRowHeight(35);
        
        // Center align all columns except Action
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columns.length - 1; i++) {
            orderTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        // Custom renderer and editor for Action column
        orderTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        orderTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        
        filterPanel.add(new JLabel("Filter Status:"));
        
        statusFilter = new JComboBox<>(new String[]{"Semua", "pending", "completed", "cancelled"});
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
        
        if ("Semua".equals(selectedStatus)) {
            orders = orderDAO.getAllOrders();
        } else {
            orders = orderDAO.getOrdersByStatus(selectedStatus);
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        
        for (Order order : orders) {
            // Format display values
            String orderType = order.getOrderType().equals("DINE_IN") ? 
                "Makan di Tempat" : "Bawa Pulang";
            
            String payment = order.getPaymentMethod().equals("CASH") ? 
                "Tunai" : "BCA (+$" + order.getAdminFee() + ")";
            
            tableModel.addRow(new Object[]{
                order.getId(),
                String.format("$%.2f", order.getTotal()),
                capitalize(order.getStatus()),
                orderType,
                payment,
                order.getCreatedAt().format(formatter),
                "Detail"
            });
        }
    }
    
    private void showOrderDetails(Order order) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Detail Order #" + order.getId());
        dialog.setModal(true);
        dialog.setSize(500, 450);
        dialog.setLayout(new BorderLayout(10, 10));
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Order Information
        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Informasi Order"));
        
        addInfoRow(infoPanel, "ID Order:", String.valueOf(order.getId()));
        addInfoRow(infoPanel, "Status:", createStatusComboBox(order));
        addInfoRow(infoPanel, "Tipe Order:", order.getOrderType().equals("DINE_IN") ? "Makan di Tempat" : "Bawa Pulang");
        addInfoRow(infoPanel, "Pembayaran:", order.getPaymentMethod().equals("CASH") ? 
            "Tunai" : "BCA (+$" + order.getAdminFee() + ")");
        addInfoRow(infoPanel, "Dibuat:", order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        addInfoRow(infoPanel, "Total:", String.format("$%.2f", order.getTotal()));
        
        // Items list
        JTextArea itemsArea = new JTextArea(8, 30);
        itemsArea.setEditable(false);
        itemsArea.setBorder(BorderFactory.createTitledBorder("Daftar Menu"));
        
        StringBuilder itemsText = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsText.append(String.format("- %s \t(x%d) \t$%.2f\n", 
                item.getMenuItem().getName(),
                item.getQuantity(),
                item.getPriceAtOrder() * item.getQuantity()));
        }
        itemsArea.setText(itemsText.toString());
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveBtn = new JButton("Simpan Perubahan");
        saveBtn.addActionListener(e -> {
            order.setStatus((String) ((JComboBox<?>) infoPanel.getComponent(3)).getSelectedItem());
            if (orderDAO.saveOrder(order)) {
                JOptionPane.showMessageDialog(dialog, "Order berhasil diperbarui", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                refreshOrderData();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Gagal memperbarui order", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton closeBtn = new JButton("Tutup");
        closeBtn.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(closeBtn);
        
        // Add components
        mainPanel.add(infoPanel);
        mainPanel.add(new JScrollPane(itemsArea));
        mainPanel.add(buttonPanel);
        
        dialog.add(mainPanel);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private void addInfoRow(JPanel panel, String label, String value) {
        panel.add(new JLabel(label));
        panel.add(new JLabel(value));
    }
    
    private void addInfoRow(JPanel panel, String label, JComponent component) {
        panel.add(new JLabel(label));
        panel.add(component);
    }
    
    private JComboBox<String> createStatusComboBox(Order order) {
        JComboBox<String> comboBox = new JComboBox<>(new String[]{"pending", "completed", "cancelled"});
        comboBox.setSelectedItem(order.getStatus());
        return comboBox;
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
            setBackground(new Color(70, 130, 180));
            setForeground(Color.WHITE);
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
            showOrderDetails(order);
            return button.getText();
        }
    }
}