package com.foodDelivery.restaurantService.repository;

import com.foodDelivery.restaurantService.model.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MenuItemRepository extends MongoRepository<MenuItem, String> {
    List<MenuItem> findByRestaurantId(String restaurantId);
    List<MenuItem> findByCategoryId(String categoryId);
    boolean existsByRestaurantIdAndNameIgnoreCase(String restaurantId, String name);
}