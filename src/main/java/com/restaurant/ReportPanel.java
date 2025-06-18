package com.restaurant;

import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.swing.border.*;
import com.toedter.calendar.JDateChooser;
import org.hibernate.*;
import org.hibernate.query.Query;

public class ReportPanel extends JPanel {
    private final JTable reportTable;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> reportTypeCombo;
    private final JDateChooser dateFromChooser;
    private final JCheckBox showAllCheckBox;

    public ReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Report type selection
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.add(new JLabel("Jenis Laporan:"));

        reportTypeCombo = new JComboBox<>(new String[]{
            "Ringkasan Penjualan", 
            "Menu Populer", 
            "Ringkasan Status Order"
        });
        filterPanel.add(reportTypeCombo);

        // Date filter
        filterPanel.add(new JLabel("Dari Tanggal:"));
        dateFromChooser = new JDateChooser();
        dateFromChooser.setDateFormatString("dd-MM-yyyy");
        dateFromChooser.setDate(new Date());
        filterPanel.add(dateFromChooser);

        // Show all checkbox
        showAllCheckBox = new JCheckBox("Tampilkan Semua");
        showAllCheckBox.addActionListener(e -> {
            dateFromChooser.setEnabled(!showAllCheckBox.isSelected());
            generateReport();
        });
        filterPanel.add(showAllCheckBox);

        JButton generateBtn = new JButton("Generate Laporan");
        generateBtn.addActionListener(e -> generateReport());
        filterPanel.add(generateBtn);

        // Table setup
        String[] columns = {"Periode", "Jumlah Order", "Total Pendapatan", "Rata-rata per Order"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(tableModel);

        // Center header kolom
        ((DefaultTableCellRenderer) reportTable.getTableHeader().getDefaultRenderer())
            .setHorizontalAlignment(JLabel.CENTER);

        // Add components
        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);

        // Generate initial report
        generateReport();
    }

    private void generateReport() {
        try {
            String reportType = (String) reportTypeCombo.getSelectedItem();
            tableModel.setRowCount(0);

            LocalDate fromDate = null;
            if (!showAllCheckBox.isSelected() && dateFromChooser.getDate() != null) {
                fromDate = dateFromChooser.getDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }

            switch (reportType) {
                case "Ringkasan Penjualan":
                    generateSalesSummary(fromDate);
                    break;
                case "Menu Populer":
                    generatePopularItems(fromDate);
                    break;
                case "Ringkasan Status Order":
                    generateStatusSummary(fromDate);
                    break;
            }

            centerTableContent();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating report: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void generateSalesSummary(LocalDate fromDate) {
        tableModel.setColumnIdentifiers(new String[]{"Tanggal", "Tipe Order", "Pembayaran", "Jumlah Order", "Total Pendapatan"});

        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            String hql = "SELECT CAST(o.createdAt AS date), " +
                       "o.orderType, o.paymentMethod, COUNT(o.id), SUM(o.total) FROM Order o ";

            if (fromDate != null) {
                hql += "WHERE CAST(o.createdAt AS date) >= :startDate ";
            }

            hql += "GROUP BY CAST(o.createdAt AS date), o.orderType, o.paymentMethod " +
                   "ORDER BY CAST(o.createdAt AS date)";

            Query<Object[]> query = session.createQuery(hql, Object[].class);

            if (fromDate != null) {
                query.setParameter("startDate", java.sql.Date.valueOf(fromDate));
            }

            List<Object[]> results = query.list();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            for (Object[] row : results) {
                java.sql.Date date = (java.sql.Date) row[0];
                LocalDate reportDate = date.toLocalDate();

                tableModel.addRow(new Object[]{
                    reportDate.format(formatter),
                    row[1].equals("DINE_IN") ? "Makan di Tempat" : "Bawa Pulang",
                    row[2].equals("CASH") ? "Tunai" : "BCA",
                    row[3],
                    String.format("Rp%,.2f", row[4])
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating sales summary: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void generatePopularItems(LocalDate fromDate) {
        tableModel.setColumnIdentifiers(new String[]{"Menu", "Kategori", "Jumlah Terjual", "Total Pendapatan"});

        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            String hql = "SELECT m.name, m.category, SUM(oi.quantity), SUM(oi.priceAtOrder * oi.quantity) " +
                       "FROM OrderItem oi JOIN oi.menuItem m JOIN oi.order o ";

            if (fromDate != null) {
                hql += "WHERE CAST(o.createdAt AS date) >= :startDate ";
            }

            hql += "GROUP BY m.name, m.category ORDER BY SUM(oi.quantity) DESC";

            Query<Object[]> query = session.createQuery(hql, Object[].class);

            if (fromDate != null) {
                query.setParameter("startDate", java.sql.Date.valueOf(fromDate));
            }

            List<Object[]> results = query.list();

            for (Object[] row : results) {
                tableModel.addRow(new Object[]{
                    row[0], row[1], row[2], String.format("Rp%,.2f", row[3])
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating popular items report: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void generateStatusSummary(LocalDate fromDate) {
        tableModel.setColumnIdentifiers(new String[]{"Status", "Jumlah Order", "Total Pendapatan", "Persentase"});

        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            String hqlTotal = "SELECT COUNT(o.id), SUM(o.total) FROM Order o ";
            if (fromDate != null) {
                hqlTotal += "WHERE CAST(o.createdAt AS date) >= :startDate ";
            }

            Query<Object[]> totalQuery = session.createQuery(hqlTotal, Object[].class);
            if (fromDate != null) {
                totalQuery.setParameter("startDate", java.sql.Date.valueOf(fromDate));
            }

            Object[] totalResult = totalQuery.uniqueResult();
            Long totalCount = (totalResult[0] != null) ? (Long) totalResult[0] : 0L;
            if (totalCount == 0) totalCount = 1L;

            String hql = "SELECT o.status, COUNT(o.id), SUM(o.total) FROM Order o ";
            if (fromDate != null) {
                hql += "WHERE CAST(o.createdAt AS date) >= :startDate ";
            }
            hql += "GROUP BY o.status";

            Query<Object[]> query = session.createQuery(hql, Object[].class);
            if (fromDate != null) {
                query.setParameter("startDate", java.sql.Date.valueOf(fromDate));
            }

            List<Object[]> results = query.list();

            for (Object[] row : results) {
                double percentage = ((Long)row[1]).doubleValue() / totalCount * 100;
                tableModel.addRow(new Object[]{
                    capitalize((String)row[0]),
                    row[1],
                    String.format("Rp%,.2f", row[2]),
                    String.format("%.1f%%", percentage)
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating status summary: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void centerTableContent() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < reportTable.getColumnCount(); i++) {
            reportTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
