package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.dto.CuisineTypeCreateRequest;
import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.CuisineTypeUpdateRequest;
import com.foodDelivery.restaurantService.mapper.CuisineTypeMapper;
import com.foodDelivery.restaurantService.model.CuisineType;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.CuisineTypeRepository;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import com.foodDelivery.restaurantService.serviceInterfaces.CuisineTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuisineTypeServiceImpl implements CuisineTypeService {

    private final CuisineTypeRepository cuisineTypeRepository;
    private final CuisineTypeMapper cuisineTypeMapper;
    private final RestaurantRepository restaurantRepository;

    @Override
    public CuisineTypeResponse createCuisineType(CuisineTypeCreateRequest request) {
        // Validate the request
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Cuisine type name is required");
        }

        // Check if a cuisine type with the same name already exists
        Optional<CuisineType> existingCuisineType = cuisineTypeRepository.findByName(request.getName());
        if (existingCuisineType.isPresent()) {
            throw new IllegalArgumentException("Cuisine type with name " + request.getName() + " already exists");
        }

        // Create and save the cuisine type
        CuisineType cuisineType = cuisineTypeMapper.toEntity(request);
        long now = System.currentTimeMillis();
        cuisineType.setCreatedAt(now);
        cuisineType.setUpdatedAt(now);
        CuisineType saved = cuisineTypeRepository.save(cuisineType);
        return cuisineTypeMapper.toDto(saved);
    }

    @Override
    public Optional<CuisineTypeResponse> getCuisineTypeById(String id) {
        return cuisineTypeRepository.findById(id)
                .map(cuisineType -> {
                    CuisineTypeResponse response = cuisineTypeMapper.toDto(cuisineType);
                    populateRestaurants(response, cuisineType.getRestaurantIds());
                    return response;
                });
    }

    @Override
    public List<CuisineTypeResponse> getAllCuisineTypes() {
        List<CuisineType> cuisineTypes = cuisineTypeRepository.findAll();
        return cuisineTypes.stream()
                .map(cuisineType -> {
                    CuisineTypeResponse response = cuisineTypeMapper.toDto(cuisineType);
                    populateRestaurants(response, cuisineType.getRestaurantIds());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void populateRestaurants(CuisineTypeResponse response, List<String> restaurantIds) {
        if (restaurantIds != null && !restaurantIds.isEmpty()) {
            List<Restaurant> restaurants = restaurantRepository.findAllById(restaurantIds);
            response.setRestaurants(restaurants.stream()
                    .map(restaurant -> {
                        CuisineTypeResponse.RestaurantSummary summary = new CuisineTypeResponse.RestaurantSummary();
                        summary.setId(restaurant.getId());
                        summary.setName(restaurant.getName());
                        summary.setImageUrl(restaurant.getRestaurantImageUrl());
                        summary.setLatitude(restaurant.getLatitude());
                        summary.setLongitude(restaurant.getLongitude());
                        summary.setFormattedAddress(restaurant.getFormattedAddress());
                        summary.setStatus(restaurant.isEnabled());
                        return summary;
                    })
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public CuisineTypeResponse updateCuisineType(String id, CuisineTypeUpdateRequest request) {
        CuisineType cuisineType = cuisineTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cuisine type not found with id: " + id));

        cuisineTypeMapper.updateEntityFromDto(request, cuisineType);
        cuisineType.setUpdatedAt(System.currentTimeMillis());
        CuisineType updated = cuisineTypeRepository.save(cuisineType);
        return cuisineTypeMapper.toDto(updated);
    }

    @Override
    public void deleteCuisineType(String id) {
        cuisineTypeRepository.deleteById(id);
    }

    @Override
    public void addRestaurantToCuisineType(String cuisineTypeId, String restaurantId) {
        CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                .orElseThrow(() -> new RuntimeException("Cuisine type not found with id: " + cuisineTypeId));

        if (!cuisineType.getRestaurantIds().contains(restaurantId)) {
            cuisineType.getRestaurantIds().add(restaurantId);
            cuisineType.setUpdatedAt(System.currentTimeMillis());
            cuisineTypeRepository.save(cuisineType);
        }
    }

    @Override
    public void removeRestaurantFromCuisineType(String cuisineTypeId, String restaurantId) {
        CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                .orElseThrow(() -> new RuntimeException("Cuisine type not found with id: " + cuisineTypeId));

        if (cuisineType.getRestaurantIds().contains(restaurantId)) {
            cuisineType.getRestaurantIds().remove(restaurantId);
            cuisineType.setUpdatedAt(System.currentTimeMillis());
            cuisineTypeRepository.save(cuisineType);
        }
    }
}