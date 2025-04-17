package com.foodDelivery.restaurantService.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantEvent {
    private String eventId;
    private String eventType;
    private String restaurantId;
    private String restaurantName;
    private String restaurantEmail;
    private String restaurantPhone;
    private List<String> adminIds;
    private List<String> addedAdminIds;
    private List<String> removedAdminIds;
    private List<String> cuisineTypeIds;
    private Map<String, Object> additionalData;
    private long timestamp;
}