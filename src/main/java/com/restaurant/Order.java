package com.restaurant;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    private double total;
    private String status = "pending";
    
    @Column(name = "order_type")
    private String orderType; // "DINE_IN" or "TAKE_AWAY"
    
    @Column(name = "payment_method")
    private String paymentMethod; // "CASH" or "BCA"
    
    @Column(name = "admin_fee")
    private double adminFee;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void calculateTotal() {
        this.total = items.stream()
            .mapToDouble(item -> item.getPriceAtOrder() * item.getQuantity())
            .sum();
        this.updatedAt = LocalDateTime.now();
    }
    
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        calculateTotal();
    }

    // Getters and Setters
    public int getId() { return id; }
    public double getTotal() { return total; }
    public String getStatus() { return status; }
    public String getOrderType() { return orderType; }
    public String getPaymentMethod() { return paymentMethod; }
    public double getAdminFee() { return adminFee; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return items; }

    public void setTotal(double total) { 
        this.total = total; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setStatus(String status) { 
        this.status = status; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setOrderType(String orderType) { 
        this.orderType = orderType; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setPaymentMethod(String paymentMethod) { 
        this.paymentMethod = paymentMethod; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setAdminFee(double adminFee) { 
        this.adminFee = adminFee; 
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}