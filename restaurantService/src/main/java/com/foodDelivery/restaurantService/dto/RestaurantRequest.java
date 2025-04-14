package com.foodDelivery.restaurantService.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class RestaurantRequest {
    @NotBlank(message = "Restaurant name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Address is required")
    @Size(max = 200, message = "Address cannot exceed 200 characters")
    private String address;

    private List<String> imageUrls = new ArrayList<>();

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid")
    private String phoneNumber;

    @Email(message = "Email must be valid")
    private String email;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Location type is required")
    @Pattern(regexp = "^(Point|Polygon)$", message = "Location type must be either 'Point' or 'Polygon'")
    private String locationType;

    @Valid
    private List<OpeningHourDto> openingHours = new ArrayList<>();

    private List<String> cuisineTypes = new ArrayList<>();

    // Optional owner ID - only used by admins
    private String ownerId;

    @Data
    public static class OpeningHourDto {
        @Min(value = 0, message = "Day of week must be between 0 and 6")
        @Max(value = 6, message = "Day of week must be between 0 and 6")
        private int dayOfWeek;

        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Open time must be in format HH:MM")
        private String openTime;

        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Close time must be in format HH:MM")
        private String closeTime;

        private boolean closed;
    }
}