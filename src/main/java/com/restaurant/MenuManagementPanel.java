package com.restaurant;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import javax.swing.table.DefaultTableCellRenderer;

public class MenuManagementPanel extends JPanel {
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final JTable menuTable;
    private final DefaultTableModel tableModel;

    public MenuManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Table setup
        String[] columns = {"ID", "Name", "Price", "Category", "Available", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4 || column == 5;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 4 ? Boolean.class : Object.class;
            }
        };

        menuTable = new JTable(tableModel);
        menuTable.setRowHeight(30);
        menuTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        menuTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        for (int i = 0; i < 5; i++) {
            menuTable.getColumnModel().getColumn(i).setCellRenderer(new CenterRenderer());
        }

        // Top toolbar like ReportPanel
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshMenuData());

        JButton addBtn = new JButton("Add New Menu");
        addBtn.addActionListener(e -> showAddEditDialog(null));

        toolBar.add(refreshBtn);
        toolBar.add(addBtn);

        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(menuTable), BorderLayout.CENTER);

        refreshMenuData();
    }

    private void refreshMenuData() {
        tableModel.setRowCount(0);
        List<MenuItem> items = menuItemDAO.getAllMenuItems(false);

        for (MenuItem item : items) {
            tableModel.addRow(new Object[]{
                item.getId(),
                item.getName(),
                String.format("$%.2f", item.getPrice()),
                item.getCategory(),
                item.isAvailable(),
                "Edit/Delete"
            });
        }
    }

    private void showAddEditDialog(MenuItem existingItem) {
        JDialog dialog = new JDialog();
        dialog.setTitle(existingItem == null ? "Add New Menu" : "Edit Menu");
        dialog.setModal(true);
        dialog.setSize(400, 500);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField categoryField = new JTextField();
        JCheckBox availableCheck = new JCheckBox("Available");
        JTextArea descriptionArea = new JTextArea(5, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionArea);

        if (existingItem != null) {
            nameField.setText(existingItem.getName());
            priceField.setText(String.valueOf(existingItem.getPrice()));
            categoryField.setText(existingItem.getCategory());
            availableCheck.setSelected(existingItem.isAvailable());
            descriptionArea.setText(existingItem.getDescription());
        }

        formPanel.add(createLabelFieldPanel("Name:", nameField));
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(createLabelFieldPanel("Price:", priceField));
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(createLabelFieldPanel("Category:", categoryField));
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(createLabelFieldPanel("Available:", availableCheck));
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(createLabelFieldPanel("Description:", descScroll));

        JPanel buttonPanel = new JPanel();
        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            try {
                MenuItem item = existingItem != null ? existingItem : new MenuItem();
                item.setName(nameField.getText());
                item.setPrice(Double.parseDouble(priceField.getText()));
                item.setCategory(categoryField.getText());
                item.setAvailable(availableCheck.isSelected());
                item.setDescription(descriptionArea.getText());

                if (menuItemDAO.saveMenuItem(item)) {
                    refreshMenuData();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save menu item", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "please enter menu properly", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel createLabelFieldPanel(String labelText, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(labelText), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private void deleteMenuItem(int id) {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this menu item?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (menuItemDAO.deleteMenuItem(id)) {
                refreshMenuData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete menu item", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

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
            int menuId = (int) tableModel.getValueAt(editingRow, 0);
            MenuItem item = menuItemDAO.getMenuItemById(menuId);

            if (button.getText().equals("Edit/Delete")) {
                int option = JOptionPane.showOptionDialog(
                    MenuManagementPanel.this,
                    "Choose action for " + item.getName(),
                    "Menu Action",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Edit", "Delete", "Cancel"},
                    "Edit"
                );

                if (option == 0) {
                    showAddEditDialog(item);
                } else if (option == 1) {
                    deleteMenuItem(item.getId());
                }
            }

            return button.getText();
        }
    }

    class CenterRenderer extends DefaultTableCellRenderer {
        public CenterRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }
    }
}
