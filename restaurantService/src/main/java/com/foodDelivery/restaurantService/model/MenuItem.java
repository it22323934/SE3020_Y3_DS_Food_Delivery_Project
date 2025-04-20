package com.foodDelivery.restaurantService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "menuItems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItem {
    @Id
    private String id;
    private String restaurantId;
    private String categoryId;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private boolean available = true;

    // Item type: DISH, GROCERY, BEVERAGE, etc.
    private String itemType;

    // Dietary preferences
    private boolean vegetarian = false;
    private boolean nonVegetarian = false;
    private boolean vegan = false;
    private boolean glutenFree = false;

    // Grocery specific attributes
    private double quantity;
    private Long expiryDate; // timestamp

    // Promotional information
    private boolean onPromotion = false;
    private double discountPercentage = 0.0;
    private double discountedPrice;

    // Additional features
    private int preparationTimeMinutes;
    private boolean spicy = false;
    private int popularityScore = 0;

    // Tracking
    private long createdAt;
    private long updatedAt;
}