package com.foodDelivery.restaurantService.repository;

import com.foodDelivery.restaurantService.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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
    @Query("{ 'enabled': true, 'location': { $near: { $geometry: { type: 'Point', coordinates: [ ?1, ?0 ] }, $maxDistance: ?2 } } }")
    List<Restaurant> findNearbyRestaurants(double latitude, double longitude, double radius);
}