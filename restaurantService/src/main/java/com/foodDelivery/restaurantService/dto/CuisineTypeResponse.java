package com.foodDelivery.restaurantService.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CuisineTypeResponse {
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private boolean active;
    private List<RestaurantSummary> restaurants = new ArrayList<>();
    private long createdAt;
    private long updatedAt;

    @Data
    public static class RestaurantSummary {
        private String id;
        private String name;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
        private boolean enabled;
        private String formattedAddress;
    }
}