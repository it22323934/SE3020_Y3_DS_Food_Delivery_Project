package com.foodDelivery.restaurantService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "restaurants")
public class Restaurant {
    @Id
    private String id;
    private String name;
    private String description;
    private String address;
    private List<String> imageUrls = new ArrayList<>();
    private String phoneNumber;
    private String email;

    // Embedded location fields
    private Double latitude;
    private Double longitude;
    private String locationType = "Point";
    private String formattedAddress;

    // Opening hours directly in restaurant
    private List<OpeningHourInfo> openingHours = new ArrayList<>();

    private List<String> cuisineTypes = new ArrayList<>();
    private boolean enabled = true;
    private String ownerId;  // User ID of the restaurant admin
    private List<String> managerIds = new ArrayList<>(); // Additional managers
    private double avgRating;
    private int totalRatings;
    private long createdAt;
    private long updatedAt;

    @Data
    public static class OpeningHourInfo {
        private int dayOfWeek; // 1-7 (Monday-Sunday)
        private String openTime; // HH:mm format
        private String closeTime; // HH:mm format
        private boolean closed;
    }
}