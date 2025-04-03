package com.foodDelivery.restaurantService.dto;

import com.foodDelivery.restaurantService.model.Restaurant;
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
public class RestaurantRequest {
    private String name;
    private String description;
    private String address;
    private String phoneNumber;
    private String email;

    // Location fields
    private Double latitude;
    private Double longitude;
    private String formattedAddress;

    // Opening hours
    private List<OpeningHourInfo> openingHours = new ArrayList<>();

    private List<String> cuisineTypes = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpeningHourInfo {
        private int dayOfWeek; // 1-7 (Monday-Sunday)
        private String openTime; // HH:mm format
        private String closeTime; // HH:mm format
        private boolean closed;
    }
}