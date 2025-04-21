package com.foodDelivery.restaurantService.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
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
    private String unit; // e.g., kg, g, lb, oz
    private double quantity;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date expiryDate; // timestamp

    // Promotional information
    private boolean onPromotion = false;
    private double discountPercentage = 0.0;
    private double discountedPrice;

    // Additional features
    private int preparationTimeMinutes;
    private boolean spicy = false;
    private int popularityScore = 0;

    // Add-ons for customization
    private List<AddOn> addOns = new ArrayList<>();

    // Tracking
    private long createdAt;
    private long updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddOn {
        private String id;
        private String name;
        private String description;
        private double price;
        private boolean available = true;
        private boolean multiple = false; // Can select multiple of this add-on
        private boolean required = false; // Is this add-on required
        private int maxQuantity = 1; // Maximum quantity allowed
    }
}