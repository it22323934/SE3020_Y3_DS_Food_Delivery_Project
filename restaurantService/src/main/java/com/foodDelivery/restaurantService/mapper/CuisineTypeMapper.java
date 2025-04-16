package com.foodDelivery.restaurantService.mapper;

import com.foodDelivery.restaurantService.dto.CuisineTypeCreateRequest;
import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.CuisineTypeUpdateRequest;
import com.foodDelivery.restaurantService.model.CuisineType;
import org.springframework.stereotype.Component;

@Component
public class CuisineTypeMapper {

    public CuisineType toEntity(CuisineTypeCreateRequest request) {
        CuisineType cuisineType = new CuisineType();
        cuisineType.setName(request.getName());
        cuisineType.setDescription(request.getDescription());
        cuisineType.setIconUrl(request.getIconUrl());
        cuisineType.setActive(request.isActive());
        return cuisineType;
    }

    public void updateEntityFromDto(CuisineTypeUpdateRequest request, CuisineType cuisineType) {
        if (request.getName() != null) {
            cuisineType.setName(request.getName());
        }
        if (request.getDescription() != null) {
            cuisineType.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            cuisineType.setIconUrl(request.getIconUrl());
        }
        if (request.getActive() != null) {
            cuisineType.setActive(request.getActive());
        }
    }

    public CuisineTypeResponse toDto(CuisineType cuisineType) {
        CuisineTypeResponse response = new CuisineTypeResponse();
        response.setId(cuisineType.getId());
        response.setName(cuisineType.getName());
        response.setDescription(cuisineType.getDescription());
        response.setIconUrl(cuisineType.getIconUrl());
        response.setActive(cuisineType.isActive());
        response.setCreatedAt(cuisineType.getCreatedAt());
        response.setUpdatedAt(cuisineType.getUpdatedAt());
        return response;
    }
}