package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.mapper.RestaurantTypeMapper;
import com.foodDelivery.restaurantService.model.MenuCategory;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.serviceInterfaces.CuisineTypeService;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuCategoryService;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuItemService;
import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants/all-users-access")
@RequiredArgsConstructor
@Slf4j
public class RestaurantUserViewController {
    private final RestaurantService restaurantService;
    private final CuisineTypeService cuisineTypeService;
    private final MenuItemService menuItemService;
    private final MenuCategoryService menuCategoryService;

    @GetMapping("/all")
    public ResponseEntity<List<RestaurantResponse>> getAllEnabledRestaurants() {
        log.info("Fetching all enabled restaurants");
        return ResponseEntity.ok(restaurantService.getAllEnabledRestaurants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable String id) {
        try {
            Restaurant restaurant = restaurantService.getRestaurantById(id);
            return ResponseEntity.ok(RestaurantTypeMapper.mapToResponse(restaurant));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestaurantResponse());
        }
    }

    @GetMapping("/cuisine-types/active")
    public ResponseEntity<List<CuisineTypeResponse>> getActiveCuisineTypes() {
        log.info("Fetching all active cuisine types");
        try {
            List<CuisineTypeResponse> activeCuisineTypes = cuisineTypeService.getActiveCuisineTypes();
            return ResponseEntity.ok(activeCuisineTypes);
        } catch (Exception e) {
            log.error("Error fetching active cuisine types: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cuisine-types/{id}")
    public ResponseEntity<?> getCuisineTypeById(@PathVariable String id) {
        log.info("Fetching cuisine type with id: {}", id);
        return cuisineTypeService.getCuisineTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/menu-categories/by-restaurant/{restaurantId}")
    public ResponseEntity<List<MenuCategory>> getCategoriesByRestaurantId(@PathVariable String restaurantId) {
        log.info("Fetching active menu categories for restaurant: {}", restaurantId);
        List<MenuCategory> activeCategories = menuCategoryService.getActiveCategoriesByRestaurantId(restaurantId);
        return ResponseEntity.ok(activeCategories);
    }

    @GetMapping("/menu-items/restaurant/{restaurantId}")
    public ResponseEntity<?> getMenuItemsByRestaurantId(@PathVariable String restaurantId) {
        try {
            log.info("Fetching available menu items for restaurant: {}", restaurantId);
            List<MenuItemResponse> availableItems = menuItemService.getAvailableMenuItemsByRestaurantId(restaurantId);
            return ResponseEntity.ok(availableItems);
        } catch (Exception e) {
            log.error("Error fetching menu items: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/menu-items/category/{categoryId}")
    public ResponseEntity<?> getMenuItemsByCategoryId(@PathVariable String categoryId) {
        try {
            log.info("Fetching available menu items for category: {}", categoryId);
            List<MenuItemResponse> availableItems = menuItemService.getAvailableMenuItemsByCategoryId(categoryId);
            return ResponseEntity.ok(availableItems);
        } catch (Exception e) {
            log.error("Error fetching menu items: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyRestaurants(
            @RequestParam(name = "lat") double latitude,
            @RequestParam(name = "lng") double longitude,
            @RequestParam(defaultValue = "10") double radius) {
        try {
            List<RestaurantResponse> restaurants = restaurantService.getNearbyRestaurants(
                    latitude, longitude, radius);
            return ResponseEntity.ok(restaurants);
        } catch (BusinessValidationException e) {
            log.warn("Invalid parameters for nearby restaurants: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
