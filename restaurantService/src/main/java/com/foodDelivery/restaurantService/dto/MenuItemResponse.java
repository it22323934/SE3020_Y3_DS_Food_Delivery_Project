package com.foodDelivery.restaurantService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String unit;
    private double quantity;
    private Long expiryDate;
    private boolean onPromotion;
    private double discountPercentage;
    private double discountedPrice;
    private int preparationTimeMinutes;
    private boolean spicy;
    private int popularityScore;
    private List<AddOnResponse> addOns = new ArrayList<>();
    private long createdAt;
    private long updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddOnResponse {
        private String id;
        private String name;
        private String description;
        private double price;
        private boolean available;
        private boolean multiple;
        private boolean required;
        private int maxQuantity;
    }
}