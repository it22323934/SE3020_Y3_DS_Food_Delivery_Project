package com.foodDelivery.restaurantService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class MenuCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;

    @PositiveOrZero(message = "Display order must be positive or zero")
    private int displayOrder;

    private boolean active = true;
}