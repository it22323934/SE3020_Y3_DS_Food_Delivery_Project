package com.foodDelivery.restaurantService.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RestaurantRequest {
    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    private String restaurantImageUrl;
    private String bannerImageUrl;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s./0-9]*$", message = "Invalid phone number format")
    private String phoneNumber;

    @Email(message = "Please provide a valid email address")
    private String email;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private String formattedAddress;

    @Valid
    private List<OpeningHourDto> openingHours = new ArrayList<>();

    @NotEmpty(message = "At least one cuisine type must be selected")
    private List<String> cuisineTypeIds = new ArrayList<>();

    @NotEmpty(message = "At least one admin must be selected")
    private List<String> adminIds = new ArrayList<>();

    @Valid
    private boolean enabled;

    @Data
    public static class OpeningHourDto {
        @Min(value = 1, message = "Day of week must be between 1 and 7")
        @Max(value = 7, message = "Day of week must be between 1 and 7")
        private int dayOfWeek;

        private String openTime;
        private String closeTime;
        private boolean closed;
    }
}