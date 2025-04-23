package com.foodDelivery.restaurantService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
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
    private String restaurantImageUrl;  // Main restaurant image
    private String bannerImageUrl;      // Banner image
    private String phoneNumber;
    private String email;

    // Location fields
    private Double latitude;
    private Double longitude;
    private String locationType = "Point";
    private String formattedAddress;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] location;

    // Opening hours
    private List<OpeningHourInfo> openingHours = new ArrayList<>();

    // References to categories
    private List<String> cuisineTypeIds = new ArrayList<>();

    private boolean enabled = true;
    private List<String> adminIds = new ArrayList<>();  // Other admins
    private double avgRating;
    private int totalRatings;
    private long createdAt;
    private long updatedAt;

    @Data
    public static class OpeningHourInfo {
        private int dayOfWeek;
        private String openTime;
        private String closeTime;
        private boolean closed;
    }

}