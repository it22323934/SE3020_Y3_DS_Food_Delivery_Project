// MenuItemCreateRequest.java
package com.foodDelivery.restaurantService.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemCreateRequest {
    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private Double price;

    private String imageUrl;
    private Boolean available;
    private String itemType;
    private Boolean vegetarian;
    private Boolean nonVegetarian;
    private Boolean vegan;
    private Boolean glutenFree;
    private Double quantity;
    private Long expiryDate;
    private Boolean onPromotion;
    private Double discountPercentage = 0.0;
    private Integer preparationTimeMinutes;
    private Boolean spicy = false;
}