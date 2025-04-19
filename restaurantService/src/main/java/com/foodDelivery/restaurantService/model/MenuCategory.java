package com.foodDelivery.restaurantService.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "menuCategories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategory {
    @Id
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