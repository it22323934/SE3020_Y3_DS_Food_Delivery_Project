package com.foodDelivery.restaurantService.service;

import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public Restaurant createRestaurant(Restaurant restaurant) {
        // Set creation and update timestamps
        long currentTime = System.currentTimeMillis();
        restaurant.setCreatedAt(currentTime);
        restaurant.setUpdatedAt(currentTime);

        // Set default values if not provided
        if (restaurant.getDescription() == null) {
            restaurant.setDescription("");
        }

        if (restaurant.getImageUrls() == null) {
            restaurant.setImageUrls(new ArrayList<>());
        }

        if (restaurant.getCuisineTypes() == null) {
            restaurant.setCuisineTypes(new ArrayList<>());
        }

        if (restaurant.getOpeningHours() == null) {
            restaurant.setOpeningHours(createDefaultOpeningHours());
        }

        // Initialize manager IDs if null
        if (restaurant.getManagerIds() == null) {
            restaurant.setManagerIds(new ArrayList<>());
        }

        // Set restaurant to enabled by default
        restaurant.setEnabled(true);

        // Initialize ratings
        restaurant.setAvgRating(0.0);
        restaurant.setTotalRatings(0);

        // Save the restaurant
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant created with ID: {}", savedRestaurant.getId());
        return savedRestaurant;
    }

    // Helper method to create default opening hours
    private List<Restaurant.OpeningHourInfo> createDefaultOpeningHours() {
        List<Restaurant.OpeningHourInfo> openingHours = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            Restaurant.OpeningHourInfo info = new Restaurant.OpeningHourInfo();
            info.setDayOfWeek(day);
            info.setOpenTime("09:00");
            info.setCloseTime("17:00");
            info.setClosed(day == 0); // Sunday closed by default
            openingHours.add(info);
        }
        return openingHours;
    }

    public Restaurant getRestaurantById(String id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    public Restaurant updateRestaurant(String id, Restaurant restaurantDetails) {
        Restaurant restaurant = getRestaurantById(id);

        // Update fields
        restaurant.setName(restaurantDetails.getName());
        restaurant.setDescription(restaurantDetails.getDescription());
        restaurant.setAddress(restaurantDetails.getAddress());
        restaurant.setImageUrls(restaurantDetails.getImageUrls());
        restaurant.setPhoneNumber(restaurantDetails.getPhoneNumber());
        restaurant.setEmail(restaurantDetails.getEmail());
        restaurant.setLatitude(restaurantDetails.getLatitude());
        restaurant.setLongitude(restaurantDetails.getLongitude());
        restaurant.setFormattedAddress(restaurantDetails.getFormattedAddress());
        restaurant.setOpeningHours(restaurantDetails.getOpeningHours());
        restaurant.setCuisineTypes(restaurantDetails.getCuisineTypes());
        restaurant.setEnabled(restaurantDetails.isEnabled());
        restaurant.setManagerIds(restaurantDetails.getManagerIds());

        // Update timestamp
        restaurant.setUpdatedAt(System.currentTimeMillis());

        return restaurantRepository.save(restaurant);
    }

    public void deleteRestaurant(String id) {
        Restaurant restaurant = getRestaurantById(id);
        restaurantRepository.delete(restaurant);
        log.info("Restaurant deleted with ID: {}", id);
    }

    public List<Restaurant> getRestaurantsByOwnerId(String ownerId) {
        return restaurantRepository.findByOwnerId(ownerId);
    }
}