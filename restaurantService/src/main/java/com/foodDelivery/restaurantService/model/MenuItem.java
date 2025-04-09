package com.foodDelivery.restaurantService.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "menuItems")
public class MenuItem {
    @Id
    private String id;
    private String restaurantId;
    private String name;
    private String description;
    private double price;
    private String category;
    private List<String> imageUrls = new ArrayList<>();
    private boolean available = true;
    private boolean onPromotion = false;
    private double discountPercentage = 0.0;
    private double discountedPrice;
    private long createdAt;
    private long updatedAt;
}