package com.foodDelivery.restaurantService.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RestaurantResponse {
    private String id;
    private String name;
    private String description;
    private String address;
    private List<String> imageUrls = new ArrayList<>();
    private String phoneNumber;
    private String email;
    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    private List<OpeningHourDto> openingHours = new ArrayList<>();
    private List<String> cuisineTypes = new ArrayList<>();
    private boolean enabled;
    private double avgRating;
    private int totalRatings;
    private long createdAt;
    private long updatedAt;

    @Data
    public static class OpeningHourDto {
        private int dayOfWeek;
        private String openTime;
        private String closeTime;
        private boolean closed;
    }
}