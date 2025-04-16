package com.foodDelivery.restaurantService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RestaurantUpdateRequest {
    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    private String description;
    private String address;
    private String restaurantImageUrl;
    private String bannerImageUrl;
    private List<String> additionalImageUrls = new ArrayList<>();
    private String phoneNumber;
    private String email;
    private Double latitude;
    private Double longitude;
    private List<OpeningHourDto> openingHours = new ArrayList<>();
    private List<String> cuisineTypeIds = new ArrayList<>();

    @Data
    public static class OpeningHourDto {
        private int dayOfWeek;
        private String openTime;
        private String closeTime;
        private boolean closed;
    }
}