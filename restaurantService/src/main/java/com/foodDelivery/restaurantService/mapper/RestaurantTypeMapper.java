package com.foodDelivery.restaurantService.mapper;

import com.foodDelivery.restaurantService.dto.RestaurantRequest;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.model.Restaurant;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class RestaurantTypeMapper {

    public static RestaurantResponse mapToResponse(Restaurant restaurant) {
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
        response.setLocation(restaurant.getLocation());
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

    public static Restaurant mapToEntity(RestaurantRequest request) {
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

        // Set up location field for geospatial queries
        if (request.getLatitude() != null && request.getLongitude() != null) {
            double[] location = new double[] {request.getLongitude(), request.getLatitude()};
            restaurant.setLocation(location);
        }

        restaurant.setFormattedAddress(request.getFormattedAddress());
        restaurant.setEnabled(request.isEnabled());
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
}
