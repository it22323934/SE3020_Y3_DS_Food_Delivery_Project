package com.foodDelivery.restaurantService.repository;

import com.foodDelivery.restaurantService.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
    List<Restaurant> findByAdminIdsContaining(String adminId);
    List<Restaurant> findByCuisineTypeIdsContaining(String cuisineTypeId);
    List<Restaurant> findByEnabled(boolean enabled);
    boolean existsByName(String name);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}