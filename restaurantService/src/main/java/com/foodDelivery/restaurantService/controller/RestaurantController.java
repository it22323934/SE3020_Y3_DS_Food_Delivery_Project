package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.RestaurantRequest;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_RESTAURANT_ADMIN') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<RestaurantResponse> createRestaurant(@RequestBody RestaurantRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Map request to entity
        Restaurant restaurant = mapToEntity(request);
        restaurant.setOwnerId(userId);

        // Create restaurant
        Restaurant created = restaurantService.createRestaurant(restaurant);

        // Convert to response and return
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable String id) {
        Restaurant restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(mapToResponse(restaurant));
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        List<RestaurantResponse> response = restaurants.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable String id,
            @RequestBody RestaurantRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        Restaurant existing = restaurantService.getRestaurantById(id);

        // Check if user has permission to update
        if (!existing.getOwnerId().equals(userId) &&
                !existing.getManagerIds().contains(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Update restaurant
        Restaurant updated = mapToEntity(request);
        updated.setId(id);
        updated.setOwnerId(existing.getOwnerId());
        Restaurant result = restaurantService.updateRestaurant(id, updated);

        return ResponseEntity.ok(mapToResponse(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RESTAURANT_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteRestaurant(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        Restaurant existing = restaurantService.getRestaurantById(id);

        // Check if user has permission to delete
        if (!existing.getOwnerId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-restaurants")
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<RestaurantResponse>> getMyRestaurants() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        List<Restaurant> restaurants = restaurantService.getRestaurantsByOwnerId(userId);
        List<RestaurantResponse> response = restaurants.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // Helper methods for mapping
    private Restaurant mapToEntity(RestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setImageUrls(request.getImageUrls());
        restaurant.setPhoneNumber(request.getPhoneNumber());
        restaurant.setEmail(request.getEmail());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setCuisineTypes(request.getCuisineTypes());

        // Map opening hours
        if (request.getOpeningHours() != null) {
            restaurant.setOpeningHours(request.getOpeningHours().stream()
                    .map(dto -> {
                        Restaurant.OpeningHourInfo info = new Restaurant.OpeningHourInfo();
                        info.setDayOfWeek(dto.getDayOfWeek());
                        info.setOpenTime(dto.getOpenTime());
                        info.setCloseTime(dto.getCloseTime());
                        info.setClosed(dto.isClosed());
                        return info;
                    })
                    .collect(Collectors.toList()));
        }

        return restaurant;
    }

    private RestaurantResponse mapToResponse(Restaurant restaurant) {
        RestaurantResponse response = new RestaurantResponse();
        response.setId(restaurant.getId());
        response.setName(restaurant.getName());
        response.setDescription(restaurant.getDescription());
        response.setAddress(restaurant.getAddress());
        response.setImageUrls(restaurant.getImageUrls());
        response.setPhoneNumber(restaurant.getPhoneNumber());
        response.setEmail(restaurant.getEmail());
        response.setLatitude(restaurant.getLatitude());
        response.setLongitude(restaurant.getLongitude());
        response.setFormattedAddress(restaurant.getFormattedAddress());
        response.setCuisineTypes(restaurant.getCuisineTypes());
        response.setEnabled(restaurant.isEnabled());
        response.setAvgRating(restaurant.getAvgRating());
        response.setTotalRatings(restaurant.getTotalRatings());
        response.setCreatedAt(restaurant.getCreatedAt());
        response.setUpdatedAt(restaurant.getUpdatedAt());

        // Map opening hours
        if (restaurant.getOpeningHours() != null) {
            response.setOpeningHours(restaurant.getOpeningHours().stream()
                    .map(info -> {
                        RestaurantResponse.OpeningHourDto dto = new RestaurantResponse.OpeningHourDto();
                        dto.setDayOfWeek(info.getDayOfWeek());
                        dto.setOpenTime(info.getOpenTime());
                        dto.setCloseTime(info.getCloseTime());
                        dto.setClosed(info.isClosed());
                        return dto;
                    })
                    .collect(Collectors.toList()));
        }

        return response;
    }
}