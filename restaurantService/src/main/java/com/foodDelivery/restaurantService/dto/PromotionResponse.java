package com.foodDelivery.restaurantService.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PromotionResponse {
    private String id;
    private String restaurantId;
    private String code;
    private String description;
    private double discountPercentage;
    private double maxDiscount;
    private double minOrderAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private int maxUses;
    private int currentUses;
    private boolean oneTimeUsePerUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}