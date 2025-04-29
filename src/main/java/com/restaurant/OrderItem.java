package com.restaurant;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;
    
    private int quantity;
    
    @Column(name = "price_at_order")
    private double priceAtOrder;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public OrderItem() {
        this.createdAt = LocalDateTime.now();
    }

    public OrderItem(MenuItem menuItem, int quantity) {
        this();
        this.menuItem = menuItem;
        this.quantity = quantity;
        this.priceAtOrder = menuItem.getPrice();
    }

    // Getters and setters
    public int getId() { return id; }
    public Order getOrder() { return order; }
    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public double getPriceAtOrder() { return priceAtOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setOrder(Order order) { this.order = order; }
    public void setMenuItem(MenuItem menuItem) { 
        this.menuItem = menuItem;
        this.priceAtOrder = menuItem.getPrice();
    }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setPriceAtOrder(double priceAtOrder) { this.priceAtOrder = priceAtOrder; }
}