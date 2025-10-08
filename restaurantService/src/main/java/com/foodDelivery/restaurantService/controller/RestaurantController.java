package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.RestaurantRequest;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.mapper.RestaurantTypeMapper;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantController {

    private static final String RESTAURANT_SERVICE = "restaurantService";
    private final RestaurantService restaurantService;

    @PostMapping
    @CircuitBreaker(name = RESTAURANT_SERVICE, fallbackMethod = "createRestaurantFallback")
    public ResponseEntity<?> createRestaurant(
            @Valid @RequestBody RestaurantRequest request,
            @RequestHeader("Authorization") String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Restaurant restaurant = RestaurantTypeMapper.mapToEntity(request);
        Restaurant created = restaurantService.createRestaurant(restaurant, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestaurantTypeMapper.mapToResponse(created));
    }

    public ResponseEntity<?> createRestaurantFallback(
            RestaurantRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to create restaurant", e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@restaurantAuthorizationService.canModifyRestaurant(#id, authentication)")
    @CircuitBreaker(name = RESTAURANT_SERVICE, fallbackMethod = "updateRestaurantFallback")
    public ResponseEntity<?> updateRestaurant(
            @PathVariable String id,
            @Valid @RequestBody RestaurantRequest request,
            @RequestHeader("Authorization") String token) {
        Restaurant restaurant = RestaurantTypeMapper.mapToEntity(request);
        Restaurant updated = restaurantService.updateRestaurant(id, restaurant, token);
        return ResponseEntity.ok(RestaurantTypeMapper.mapToResponse(updated));
    }

    public ResponseEntity<?> updateRestaurantFallback(
            String id, RestaurantRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to update restaurant {}", id, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@restaurantAuthorizationService.canDeleteRestaurant(#id, authentication)")
    @CircuitBreaker(name = RESTAURANT_SERVICE, fallbackMethod = "deleteRestaurantFallback")
    public ResponseEntity<?> deleteRestaurant(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        restaurantService.deleteRestaurant(id, userId, token);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> deleteRestaurantFallback(
            String id, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to delete restaurant {}", id, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @GetMapping("/{id}")
    @CircuitBreaker(name = RESTAURANT_SERVICE, fallbackMethod = "getRestaurantFallback")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable String id) {
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(RestaurantTypeMapper.mapToResponse(restaurant));
    }

    public ResponseEntity<RestaurantResponse> getRestaurantFallback(String id, Exception e) {
        log.error("Circuit breaker fallback: Failed to get restaurant {}", id, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new RestaurantResponse());
    }

    @GetMapping
    @CircuitBreaker(name = RESTAURANT_SERVICE, fallbackMethod = "getAllRestaurantsFallback")
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(restaurants.stream()
                .map(RestaurantTypeMapper::mapToResponse)
                .toList());
    }

    public ResponseEntity<List<RestaurantResponse>> getAllRestaurantsFallback(Exception e) {
        log.error("Circuit breaker fallback: Failed to get all restaurants", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = RESTAURANT_SERVICE, fallbackMethod = "getRestaurantsByUserIdFallback")
    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByUserId(
            @PathVariable String userId) {
        log.info("Fetching restaurants for user ID: {}", userId);
        List<Restaurant> restaurants = restaurantService.getRestaurantsByAdminId(userId);
        List<RestaurantResponse> responses = restaurants.stream()
                .map(RestaurantTypeMapper::mapToResponse)
                .toList();
        log.info("Found {} restaurants for user ID: {}", responses.size(), userId);
        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByUserIdFallback(
            String userId, Exception e) {
        log.error("Circuit breaker fallback: Failed to get restaurants for user {}", userId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/nearby")
    @CircuitBreaker(name = RESTAURANT_SERVICE, fallbackMethod = "getNearbyRestaurantsFallback")
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
        log.error("Circuit breaker fallback: Failed to get nearby restaurants", e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }
}