package com.foodDelivery.restaurantService.repository;

import com.foodDelivery.restaurantService.model.MenuCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuCategoryRepository extends MongoRepository<MenuCategory, String> {
    List<MenuCategory> findByRestaurantId(String restaurantId);
    List<MenuCategory> findByRestaurantIdOrderByDisplayOrderAsc(String restaurantId);
    boolean existsByNameAndRestaurantId(String name, String restaurantId);
    boolean existsByNameIgnoreCaseAndRestaurantId(String name, String restaurantId);
    int countByRestaurantId(String restaurantId);
}