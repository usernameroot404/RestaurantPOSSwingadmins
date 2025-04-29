package com.restaurant;

import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrderDAO {
    private static final Logger logger = Logger.getLogger(OrderDAO.class.getName());

    public boolean saveOrder(Order order) {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                order.calculateTotal();
                session.saveOrUpdate(order);

                for (OrderItem item : order.getItems()) {
                    session.saveOrUpdate(item);
                }

                transaction.commit();
                return true;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                throw e;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Gagal menyimpan order", e);
            return false;
        }
    }
    
    public List<Order> getAllOrders() {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.items " +
                "ORDER BY o.createdAt DESC", Order.class)
                .list();
        }
    }
    
    public List<Order> getOrdersByStatus(String status) {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT DISTINCT o FROM Order o " +
                "LEFT JOIN FETCH o.items " +
                "WHERE o.status = :status " +
                "ORDER BY o.createdAt DESC", Order.class)
                .setParameter("status", status)
                .list();
        }
    }
    
    public Order getOrderById(int id) {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            return session.createQuery(
                "SELECT o FROM Order o " +
                "LEFT JOIN FETCH o.items " +
                "WHERE o.id = :id", Order.class)
                .setParameter("id", id)
                .uniqueResult();
        }
    }
}