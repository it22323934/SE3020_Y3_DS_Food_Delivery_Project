package com.foodDelivery.restaurantService.serviceInterfaces;

import com.foodDelivery.restaurantService.dto.CuisineTypeCreateRequest;
import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.CuisineTypeUpdateRequest;
import com.foodDelivery.restaurantService.model.CuisineType;

import java.util.List;
import java.util.Optional;

public interface CuisineTypeService {
    CuisineTypeResponse createCuisineType(CuisineTypeCreateRequest request);
    List<CuisineTypeResponse> getAllCuisineTypes();
    Optional<CuisineTypeResponse> getCuisineTypeById(String id);
    CuisineTypeResponse updateCuisineType(String id, CuisineTypeUpdateRequest request);
    void deleteCuisineType(String id);
    void addRestaurantToCuisineType(String cuisineTypeId, String restaurantId);
    void removeRestaurantFromCuisineType(String cuisineTypeId, String restaurantId);
    List<CuisineTypeResponse> getActiveCuisineTypes();
    List<CuisineType> getCuisineTypesByIds(List<String> cuisineTypeIds);
    List<CuisineTypeResponse> getCuisineTypesByRestaurantId(String restaurantId);
}