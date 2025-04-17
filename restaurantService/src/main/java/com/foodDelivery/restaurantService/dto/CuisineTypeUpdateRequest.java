package com.foodDelivery.restaurantService.dto;

import lombok.Data;

@Data
public class CuisineTypeUpdateRequest {
    private String name;
    private String description;
    private String iconUrl;
    private Boolean active;
}