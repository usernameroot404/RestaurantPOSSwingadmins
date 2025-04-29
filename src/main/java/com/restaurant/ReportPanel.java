package com.restaurant;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import com.toedter.calendar.JDateChooser;

public class ReportPanel extends JPanel {
    private final JTable reportTable;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> reportTypeCombo;
    private final JDateChooser dateFromChooser;
    private final JDateChooser dateToChooser;

    public ReportPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Report type selection
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Report Type:"));

        reportTypeCombo = new JComboBox<>(new String[]{
            "Sales Summary", 
            "Popular Menu Items", 
            "Order Status Summary"
        });
        filterPanel.add(reportTypeCombo);

        // Date range
        filterPanel.add(new JLabel("From:"));
        dateFromChooser = new JDateChooser();
        dateFromChooser.setDate(new Date());
        filterPanel.add(dateFromChooser);

        filterPanel.add(new JLabel("To:"));
        dateToChooser = new JDateChooser();
        dateToChooser.setDate(new Date());
        filterPanel.add(dateToChooser);

        JButton generateBtn = new JButton("Generate Report");
        generateBtn.addActionListener(e -> generateReport());
        filterPanel.add(generateBtn);

        // Table setup
        String[] columns = {"Period", "Total Orders", "Total Revenue", "Avg Order Value"};
        tableModel = new DefaultTableModel(columns, 0);
        reportTable = new JTable(tableModel);

        // Center-align table content
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        reportTable.setDefaultRenderer(Object.class, centerRenderer);

        // Add components
        add(filterPanel, BorderLayout.NORTH);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);
    }

    private void generateReport() {
        try {
            if (dateFromChooser.getDate() == null || dateToChooser.getDate() == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select both date ranges", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate fromDate = dateFromChooser.getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            LocalDate toDate = dateToChooser.getDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if (fromDate.isAfter(toDate)) {
                JOptionPane.showMessageDialog(this, 
                    "End date must be after start date", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            String reportType = (String) reportTypeCombo.getSelectedItem();
            tableModel.setRowCount(0);

            switch (reportType) {
                case "Sales Summary":
                    generateSalesSummary(fromDate, toDate);
                    break;
                case "Popular Menu Items":
                    generatePopularItems(fromDate, toDate);
                    break;
                case "Order Status Summary":
                    generateStatusSummary(fromDate, toDate);
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error generating report: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void generateSalesSummary(LocalDate fromDate, LocalDate toDate) {
        tableModel.setColumnIdentifiers(new String[]{"Date", "Total Orders", "Total Revenue", "Avg Order Value"});

        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();

            String hql = "SELECT CAST(o.createdAt AS date), " +
                       "COUNT(o.id), " +
                       "SUM(o.total), " +
                       "AVG(o.total) " +
                       "FROM Order o " +
                       "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
                       "GROUP BY CAST(o.createdAt AS date) " +
                       "ORDER BY CAST(o.createdAt AS date)";

            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("startDate", startDateTime);
            query.setParameter("endDate", endDateTime);

            List<Object[]> results = query.list();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Object[] row : results) {
                java.sql.Date date = (java.sql.Date) row[0];
                LocalDate reportDate = date.toLocalDate();

                tableModel.addRow(new Object[]{
                    reportDate.format(formatter),
                    row[1],
                    String.format("$%.2f", row[2]),
                    String.format("$%.2f", row[3])
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

    private void generatePopularItems(LocalDate fromDate, LocalDate toDate) {
        tableModel.setColumnIdentifiers(new String[]{"Menu Item", "Category", "Quantity Sold", "Total Revenue"});

        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();

            String hql = "SELECT m.name, m.category, SUM(oi.quantity), SUM(oi.priceAtOrder * oi.quantity) " +
                       "FROM OrderItem oi " +
                       "JOIN oi.menuItem m " +
                       "JOIN oi.order o " +
                       "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
                       "GROUP BY m.name, m.category " +
                       "ORDER BY SUM(oi.quantity) DESC";

            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("startDate", startDateTime);
            query.setParameter("endDate", endDateTime);

            List<Object[]> results = query.list();

            for (Object[] row : results) {
                tableModel.addRow(new Object[]{
                    row[0],
                    row[1],
                    row[2],
                    String.format("$%.2f", row[3])
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

    private void generateStatusSummary(LocalDate fromDate, LocalDate toDate) {
        tableModel.setColumnIdentifiers(new String[]{"Status", "Order Count", "Total Revenue", "Percentage"});

        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            LocalDateTime startDateTime = fromDate.atStartOfDay();
            LocalDateTime endDateTime = toDate.plusDays(1).atStartOfDay();

            String totalHql = "SELECT COUNT(o.id), SUM(o.total) " +
                           "FROM Order o " +
                           "WHERE o.createdAt BETWEEN :startDate AND :endDate";

            Query<Object[]> totalQuery = session.createQuery(totalHql, Object[].class);
            totalQuery.setParameter("startDate", startDateTime);
            totalQuery.setParameter("endDate", endDateTime);

            Object[] totalResult = totalQuery.uniqueResult();
            Long totalCount = (Long) totalResult[0];
            if (totalCount == 0) totalCount = 1L;

            String hql = "SELECT o.status, COUNT(o.id), SUM(o.total) " +
                       "FROM Order o " +
                       "WHERE o.createdAt BETWEEN :startDate AND :endDate " +
                       "GROUP BY o.status";

            Query<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("startDate", startDateTime);
            query.setParameter("endDate", endDateTime);

            List<Object[]> results = query.list();

            for (Object[] row : results) {
                double percentage = ((Long)row[1]).doubleValue() / totalCount * 100;
                tableModel.addRow(new Object[]{
                    capitalize((String)row[0]),
                    row[1],
                    String.format("$%.2f", row[2]),
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

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
