package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.RestaurantRequest;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.mapper.RestaurantTypeMapper;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<?> createRestaurant(
            @Valid @RequestBody RestaurantRequest request,
            @RequestHeader("Authorization") String token) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        try {
            Restaurant restaurant = RestaurantTypeMapper.mapToEntity(request);
            Restaurant created = restaurantService.createRestaurant(restaurant, token);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(RestaurantTypeMapper.mapToResponse(created));
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<?> updateRestaurant(
            @PathVariable String id,
            @Valid @RequestBody RestaurantRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Restaurant restaurant = RestaurantTypeMapper.mapToEntity(request);
            Restaurant updated = restaurantService.updateRestaurant(id, restaurant, token);
            return ResponseEntity.ok(RestaurantTypeMapper.mapToResponse(updated));
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRestaurant(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        try {
            restaurantService.deleteRestaurant(id, userId, token);
            return ResponseEntity.noContent().build();
        } catch (BusinessValidationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
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

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        List<RestaurantResponse> responses = restaurants.stream()
                .map(RestaurantTypeMapper::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByUserId(@PathVariable String userId) {
        try {
            log.info("Fetching restaurants for user ID: {}", userId);
            List<Restaurant> restaurants = restaurantService.getRestaurantsByAdminId(userId);

            if (restaurants.isEmpty()) {
                log.info("No restaurants found for user ID: {}", userId);
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<RestaurantResponse> responses = restaurants.stream()
                    .map(RestaurantTypeMapper::mapToResponse)
                    .collect(Collectors.toList());

            log.info("Found {} restaurants for user ID: {}", responses.size(), userId);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching restaurants for user ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
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