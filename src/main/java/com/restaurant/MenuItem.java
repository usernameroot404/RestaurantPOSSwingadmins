package com.restaurant;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    
    private String name;
    private String description;
    private double price;
    private String category;
    
    @Column(name = "is_available")
    private boolean available = true;
    
    @Column(name = "image_path")
    private String imagePath;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public MenuItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public MenuItem(String name, double price, String category) {
        this();
        this.name = name;
        this.price = price;
        this.category = category;
    }

    // Getters and setters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public boolean isAvailable() { return available; }
    public String getImagePath() { return imagePath; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setName(String name) { 
        this.name = name; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setDescription(String description) { 
        this.description = description; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setPrice(double price) { 
        this.price = price; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setCategory(String category) { 
        this.category = category; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setAvailable(boolean available) { 
        this.available = available; 
        this.updatedAt = LocalDateTime.now();
    }
    public void setImagePath(String imagePath) { 
        this.imagePath = imagePath; 
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}