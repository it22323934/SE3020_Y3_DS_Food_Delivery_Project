package com.foodDelivery.restaurantService.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCreatedEvent {
    private String eventId;
    private String eventType;
    private String restaurantId;
    private String restaurantName;
    private String code;
    private String description;
    private double discountPercentage;
    private double minOrderAmount;
    private double maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long timestamp;
}
