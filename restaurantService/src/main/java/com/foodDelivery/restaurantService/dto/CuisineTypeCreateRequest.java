package com.foodDelivery.restaurantService.dto;

import lombok.Data;

@Data
public class CuisineTypeCreateRequest {
    private String name;
    private String description;
    private String iconUrl;
    private boolean active = true;
}