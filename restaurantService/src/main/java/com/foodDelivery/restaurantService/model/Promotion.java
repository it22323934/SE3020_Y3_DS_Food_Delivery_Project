package com.foodDelivery.restaurantService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Document(collection = "promotions")
public class Promotion {
    
    @Id
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
    private Set<String> usedByUsers;
    private String restaurantName; // For email notifications
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
