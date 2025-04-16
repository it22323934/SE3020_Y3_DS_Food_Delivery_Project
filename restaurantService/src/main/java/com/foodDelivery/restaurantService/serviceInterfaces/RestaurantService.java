package com.foodDelivery.restaurantService.serviceInterfaces;

import com.foodDelivery.restaurantService.model.Restaurant;
import java.util.List;

public interface RestaurantService {
    Restaurant createRestaurant(Restaurant restaurant, String token);
    Restaurant updateRestaurant(String id, Restaurant restaurant, String token);
    Restaurant getRestaurantById(String id);
    List<Restaurant> getAllRestaurants();
    void deleteRestaurant(String id, String userId, String token);
    List<Restaurant> getRestaurantsByAdminId(String adminId);
    Restaurant addAdminToRestaurant(String restaurantId, String adminId, String token);
    Restaurant removeAdminFromRestaurant(String restaurantId, String adminId, String userId, String token);
}