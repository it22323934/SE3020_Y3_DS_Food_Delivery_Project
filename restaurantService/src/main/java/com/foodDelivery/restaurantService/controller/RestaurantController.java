package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.RestaurantRequest;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody RestaurantRequest request,
            @RequestHeader("Authorization") String token) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        try {
            Restaurant restaurant = mapToEntity(request);
            Restaurant created = restaurantService.createRestaurant(restaurant, token);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapToResponse(created));
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RestaurantResponse());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable String id,
            @Valid @RequestBody RestaurantRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Restaurant restaurant = mapToEntity(request);
            Restaurant updated = restaurantService.updateRestaurant(id, restaurant, token);
            return ResponseEntity.ok(mapToResponse(updated));
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RestaurantResponse());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable String id) {
        try {
            Restaurant restaurant = restaurantService.getRestaurantById(id);
            return ResponseEntity.ok(mapToResponse(restaurant));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new RestaurantResponse());
        }
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getAllRestaurants() {
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        List<RestaurantResponse> responses = restaurants.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-restaurants")
    public ResponseEntity<List<RestaurantResponse>> getMyRestaurants() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        List<Restaurant> restaurants = restaurantService.getRestaurantsByAdminId(userId);
        List<RestaurantResponse> responses = restaurants.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
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
                    .body(new RestaurantResponse());
        }
    }

    private Restaurant mapToEntity(RestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setRestaurantImageUrl(request.getRestaurantImageUrl());
        restaurant.setBannerImageUrl(request.getBannerImageUrl());
        restaurant.setPhoneNumber(request.getPhoneNumber());
        restaurant.setEmail(request.getEmail());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setFormattedAddress(request.getFormattedAddress());
        restaurant.setAdminIds(request.getAdminIds());
        restaurant.setCuisineTypeIds(request.getCuisineTypeIds());

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
        response.setRestaurantImageUrl(restaurant.getRestaurantImageUrl());
        response.setBannerImageUrl(restaurant.getBannerImageUrl());
        response.setAddress(restaurant.getAddress());
        response.setPhoneNumber(restaurant.getPhoneNumber());
        response.setEmail(restaurant.getEmail());
        response.setLatitude(restaurant.getLatitude());
        response.setAdminIds(restaurant.getAdminIds());
        response.setCuisineTypeIds(restaurant.getCuisineTypeIds());
        response.setLongitude(restaurant.getLongitude());
        response.setFormattedAddress(restaurant.getFormattedAddress());
        response.setEnabled(restaurant.isEnabled());
        response.setAvgRating(restaurant.getAvgRating());
        response.setTotalRatings(restaurant.getTotalRatings());
        response.setCreatedAt(restaurant.getCreatedAt());
        response.setUpdatedAt(restaurant.getUpdatedAt());

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