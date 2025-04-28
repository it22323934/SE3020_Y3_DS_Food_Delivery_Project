package com.foodDelivery.restaurantService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromotionValidationRequest {
    @NotBlank(message = "Promotion code is required")
    private String code;
    
    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private double orderAmount;
}