package com.foodDelivery.restaurantService.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class MenuCategoryResponse {
    private String id;
    private String name;
    private String description;
    private String restaurantId;
    private int displayOrder;
    private boolean active;
    private List<String> menuItemIds = new ArrayList<>();
    private long createdAt;
    private long updatedAt;
}