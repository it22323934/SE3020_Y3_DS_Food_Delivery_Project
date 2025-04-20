// MenuItemResponse.java
package com.foodDelivery.restaurantService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {
    private String id;
    private String restaurantId;
    private String categoryId;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private boolean available;
    private String itemType;
    private boolean vegetarian;
    private boolean nonVegetarian;
    private boolean vegan;
    private boolean glutenFree;
    private double quantity;
    private Long expiryDate;
    private boolean onPromotion;
    private double discountPercentage;
    private double discountedPrice;
    private int preparationTimeMinutes;
    private boolean spicy;
    private int popularityScore;
    private long createdAt;
    private long updatedAt;
}