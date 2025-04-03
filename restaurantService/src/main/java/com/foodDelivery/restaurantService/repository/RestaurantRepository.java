package com.foodDelivery.restaurantService.repository;

import com.foodDelivery.restaurantService.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
    // Additional query methods can be defined here if needed
}