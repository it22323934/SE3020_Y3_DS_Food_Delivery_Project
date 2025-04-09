package com.foodDelivery.restaurantService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRequest {
    private String restaurantId;
    private String name;
    private String description;
    private double price;
    private String category;
    private boolean available = true;
    private boolean onPromotion = false;
    private double discountPercentage = 0.0;
}