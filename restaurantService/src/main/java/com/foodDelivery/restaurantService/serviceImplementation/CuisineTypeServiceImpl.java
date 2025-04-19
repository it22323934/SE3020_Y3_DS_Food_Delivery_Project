package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.dto.CuisineTypeCreateRequest;
import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.CuisineTypeUpdateRequest;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.mapper.CuisineTypeMapper;
import com.foodDelivery.restaurantService.model.CuisineType;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.CuisineTypeRepository;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import com.foodDelivery.restaurantService.serviceInterfaces.CuisineTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CuisineTypeServiceImpl implements CuisineTypeService {

    private final CuisineTypeRepository cuisineTypeRepository;
    private final CuisineTypeMapper cuisineTypeMapper;
    private final RestaurantRepository restaurantRepository;

    private static final int MAX_CUISINE_TYPES_PER_RESTAURANT = 5;

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
                        summary.setEnabled(restaurant.isEnabled());
                        return summary;
                    })
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public CuisineTypeResponse updateCuisineType(String id, CuisineTypeUpdateRequest request) {
        CuisineType cuisineType = cuisineTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + id));

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
                .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        // Check if restaurant is disabled - only enabled restaurants can be added
        if (!restaurant.isEnabled()) {
            throw new BusinessValidationException("Restaurant is disabled and cannot be added to cuisine type");
        }

        // Check if restaurant already has maximum number of cuisine types
        int currentCuisineTypeCount = countCuisineTypesForRestaurant(restaurantId);
        if (currentCuisineTypeCount >= MAX_CUISINE_TYPES_PER_RESTAURANT &&
                !cuisineType.getRestaurantIds().contains(restaurantId)) {
            throw new BusinessValidationException("Restaurant already has the maximum of " +
                    MAX_CUISINE_TYPES_PER_RESTAURANT + " cuisine types");
        }

        // Update both sides of the relationship
        boolean updated = false;

        // Update cuisine type
        if (!cuisineType.getRestaurantIds().contains(restaurantId)) {
            cuisineType.getRestaurantIds().add(restaurantId);
            updated = true;
        }

        // Update restaurant
        if (!restaurant.getCuisineTypeIds().contains(cuisineTypeId)) {
            restaurant.getCuisineTypeIds().add(cuisineTypeId);
            updated = true;
        }

        if (updated) {
            long now = System.currentTimeMillis();
            cuisineType.setUpdatedAt(now);
            restaurant.setUpdatedAt(now);
            cuisineTypeRepository.save(cuisineType);
            restaurantRepository.save(restaurant);
            log.info("Added restaurant {} to cuisine type {}", restaurantId, cuisineTypeId);
        }
    }

    @Override
    public void removeRestaurantFromCuisineType(String cuisineTypeId, String restaurantId) {
        CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        // Prevent removal if this would leave restaurant with fewer than MIN_CUISINE_TYPES_PER_RESTAURANT
        final int MIN_CUISINE_TYPES_PER_RESTAURANT = 1; // Set to 1 as minimum requirement

        if (restaurant.getCuisineTypeIds().size() <= MIN_CUISINE_TYPES_PER_RESTAURANT &&
                restaurant.getCuisineTypeIds().contains(cuisineTypeId)) {
            throw new BusinessValidationException("Restaurant must have at least " +
                    MIN_CUISINE_TYPES_PER_RESTAURANT + " cuisine type");
        }

        // Update both sides of the relationship
        boolean updated = false;

        // Update cuisine type
        if (cuisineType.getRestaurantIds().contains(restaurantId)) {
            cuisineType.getRestaurantIds().remove(restaurantId);
            updated = true;
        }

        // Update restaurant
        if (restaurant.getCuisineTypeIds().contains(cuisineTypeId)) {
            restaurant.getCuisineTypeIds().remove(cuisineTypeId);
            updated = true;
        }

        if (updated) {
            long now = System.currentTimeMillis();
            cuisineType.setUpdatedAt(now);
            restaurant.setUpdatedAt(now);
            cuisineTypeRepository.save(cuisineType);
            restaurantRepository.save(restaurant);
            log.info("Removed restaurant {} from cuisine type {}", restaurantId, cuisineTypeId);
        }
    }

    private int countCuisineTypesForRestaurant(String restaurantId) {
        List<CuisineType> allCuisineTypes = cuisineTypeRepository.findAll();
        return (int) allCuisineTypes.stream()
                .filter(ct -> ct.getRestaurantIds() != null && ct.getRestaurantIds().contains(restaurantId))
                .count();
    }

    @Override
    public List<CuisineTypeResponse> getCuisineTypesByRestaurantId(String restaurantId) {
        List<CuisineType> allCuisineTypes = cuisineTypeRepository.findAll();
        return allCuisineTypes.stream()
                .filter(ct -> ct.getRestaurantIds() != null && ct.getRestaurantIds().contains(restaurantId))
                .map(cuisineTypeMapper::toDto)
                .collect(Collectors.toList());
    }
}