package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.mapper.RestaurantTypeMapper;
import com.foodDelivery.restaurantService.model.MenuCategory;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.serviceInterfaces.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants/all-users-access")
@RequiredArgsConstructor
@Slf4j
public class RestaurantUserViewController {

    private static final String USER_VIEW_SERVICE = "userViewService";
    private final RestaurantService restaurantService;
    private final CuisineTypeService cuisineTypeService;
    private final MenuItemService menuItemService;
    private final MenuCategoryService menuCategoryService;

    @GetMapping("/all")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getAllEnabledRestaurantsFallback")
    public ResponseEntity<List<RestaurantResponse>> getAllEnabledRestaurants() {
        log.info("Fetching all enabled restaurants");
        return ResponseEntity.ok(restaurantService.getAllEnabledRestaurants());
    }

    public ResponseEntity<List<RestaurantResponse>> getAllEnabledRestaurantsFallback(Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch enabled restaurants", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/{id}")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getRestaurantFallback")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable String id) {
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(RestaurantTypeMapper.mapToResponse(restaurant));
    }

    public ResponseEntity<RestaurantResponse> getRestaurantFallback(String id, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch restaurant {}", id, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RestaurantResponse());
    }

    @GetMapping("/cuisine-types/active")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getActiveCuisineTypesFallback")
    public ResponseEntity<List<CuisineTypeResponse>> getActiveCuisineTypes() {
        log.info("Fetching all active cuisine types");
        List<CuisineTypeResponse> activeCuisineTypes = cuisineTypeService.getActiveCuisineTypes();
        return ResponseEntity.ok(activeCuisineTypes);
    }

    public ResponseEntity<List<CuisineTypeResponse>> getActiveCuisineTypesFallback(Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch active cuisine types", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/cuisine-types/{id}")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getCuisineTypeByIdFallback")
    public ResponseEntity<?> getCuisineTypeById(@PathVariable String id) {
        log.info("Fetching cuisine type with id: {}", id);
        return cuisineTypeService.getCuisineTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getCuisineTypeByIdFallback(String id, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch cuisine type {}", id, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @GetMapping("/menu-categories/by-restaurant/{restaurantId}")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getCategoriesByRestaurantIdFallback")
    public ResponseEntity<List<MenuCategory>> getCategoriesByRestaurantId(@PathVariable String restaurantId) {
        log.info("Fetching active menu categories for restaurant: {}", restaurantId);
        List<MenuCategory> activeCategories = menuCategoryService.getActiveCategoriesByRestaurantId(restaurantId);
        return ResponseEntity.ok(activeCategories);
    }

    public ResponseEntity<List<MenuCategory>> getCategoriesByRestaurantIdFallback(String restaurantId, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch categories for restaurant {}", restaurantId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/menu-items/restaurant/{restaurantId}")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getMenuItemsByRestaurantIdFallback")
    public ResponseEntity<?> getMenuItemsByRestaurantId(@PathVariable String restaurantId) {
        log.info("Fetching available menu items for restaurant: {}", restaurantId);
        List<MenuItemResponse> availableItems = menuItemService.getAvailableMenuItemsByRestaurantId(restaurantId);
        return ResponseEntity.ok(availableItems);
    }

    public ResponseEntity<?> getMenuItemsByRestaurantIdFallback(String restaurantId, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch menu items for restaurant {}", restaurantId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/menu-items/category/{categoryId}")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getMenuItemsByCategoryIdFallback")
    public ResponseEntity<?> getMenuItemsByCategoryId(@PathVariable String categoryId) {
        log.info("Fetching available menu items for category: {}", categoryId);
        List<MenuItemResponse> availableItems = menuItemService.getAvailableMenuItemsByCategoryId(categoryId);
        return ResponseEntity.ok(availableItems);
    }

    public ResponseEntity<?> getMenuItemsByCategoryIdFallback(String categoryId, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch menu items for category {}", categoryId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/nearby")
    @CircuitBreaker(name = USER_VIEW_SERVICE, fallbackMethod = "getNearbyRestaurantsFallback")
    public ResponseEntity<?> getNearbyRestaurants(
            @RequestParam(name = "lat") double latitude,
            @RequestParam(name = "lng") double longitude,
            @RequestParam(defaultValue = "10") double radius) {
        List<RestaurantResponse> restaurants = restaurantService.getNearbyRestaurants(
                latitude, longitude, radius);
        return ResponseEntity.ok(restaurants);
    }

    public ResponseEntity<?> getNearbyRestaurantsFallback(
            double latitude, double longitude, double radius, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch nearby restaurants", e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }
}