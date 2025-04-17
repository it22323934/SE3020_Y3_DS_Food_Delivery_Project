package com.foodDelivery.restaurantService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "cuisineTypes")
public class CuisineType {
    @Id
    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private boolean active = true;

    // Reference to restaurants with this cuisine type
    private List<String> restaurantIds = new ArrayList<>();

    private long createdAt;
    private long updatedAt;
}