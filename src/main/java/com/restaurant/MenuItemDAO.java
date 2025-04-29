package com.restaurant;

import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;

public class MenuItemDAO {
    public List<MenuItem> getAllMenuItems(boolean onlyAvailable) {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            String query = onlyAvailable ? 
                "FROM MenuItem WHERE is_available = true ORDER BY category, name" : 
                "FROM MenuItem ORDER BY category, name";
            return session.createQuery(query, MenuItem.class).list();
        }
    }

    public MenuItem getMenuItemById(int id) {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            return session.get(MenuItem.class, id);
        }
    }
    
    public boolean saveMenuItem(MenuItem item) {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(item);
            transaction.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteMenuItem(int id) {
        try (Session session = KoneksiDB.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            MenuItem item = session.get(MenuItem.class, id);
            if (item != null) {
                session.delete(item);
                transaction.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}