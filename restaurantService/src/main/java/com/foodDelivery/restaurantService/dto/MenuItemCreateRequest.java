package com.foodDelivery.restaurantService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemCreateRequest {
    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be greater than or equal to 0")
    private Double price;

    private String imageUrl;
    private Boolean available = true;
    private String itemType;
    private Boolean vegetarian = false;
    private Boolean nonVegetarian = false;
    private Boolean vegan = false;
    private Boolean glutenFree = false;
    private String unit;
    private Double quantity;
    private Long expiryDate;
    private Boolean onPromotion = false;
    private Double discountPercentage = 0.0;
    private Double discountedPrice;

    @Min(value = 0, message = "Preparation time cannot be negative")
    private Integer preparationTimeMinutes;

    private Boolean spicy = false;

    @Valid
    private List<AddOnRequest> addOns = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddOnRequest {
        @NotBlank(message = "Add-on name is required")
        private String name;

        private String description;

        @NotNull(message = "Add-on price is required")
        @PositiveOrZero(message = "Add-on price must be greater than or equal to 0")
        private Double price;

        private Boolean available = true;
        private Boolean multiple = false;
        private Boolean required = false;

        @Min(value = 1, message = "Max quantity must be at least 1")
        @Max(value = 100, message = "Max quantity cannot exceed 100")
        private Integer maxQuantity = 1;
    }
}