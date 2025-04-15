package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.client.UserServiceClient;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.model.CuisineType;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.CuisineTypeRepository;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import com.foodDelivery.restaurantService.serviceInterfaces.KafkaProducerService;
import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final CuisineTypeRepository cuisineTypeRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaProducerService kafkaProducerService;

    @Override
    public Restaurant createRestaurant(Restaurant restaurant, String token) {
        // Extract user ID from authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();


        // Check if current user is a system admin
        boolean isSystemAdmin = userServiceClient.validateUserRole(currentUserId, "ROLE_ADMIN", token);

        // Validate admin IDs list
        if (restaurant.getAdminIds() == null || restaurant.getAdminIds().isEmpty()) {
            throw new BusinessValidationException("At least one restaurant admin must be specified");
        }

        // If not a system admin, only allow adding themselves as admin
        if (!isSystemAdmin) {
            // Non-admins can only specify themselves as admin
            if (restaurant.getAdminIds().size() > 1 || !restaurant.getAdminIds().contains(currentUserId)) {
                restaurant.setAdminIds(List.of(currentUserId));
                log.info("Non-admin user can only add themselves as restaurant admin. Adjusted admin list accordingly.");
            }
        } else {
            // System admin can add any restaurant admin, validate each admin ID
            for (String adminId : restaurant.getAdminIds()) {
                boolean isValidRole = userServiceClient.validateUserRoleById(adminId, "ROLE_RESTAURANT_ADMIN", token);
                if (!isValidRole) {
                    throw new BusinessValidationException("User " + adminId + " doesn't have required role to manage restaurants");
                }
            }
        }

        // Validate other restaurant data like cuisine types
        validateCuisineTypes(restaurant);

        long currentTime = System.currentTimeMillis();
        restaurant.setCreatedAt(currentTime);
        restaurant.setUpdatedAt(currentTime);
        restaurant.setEnabled(true);
        restaurant.setAvgRating(0.0);
        restaurant.setTotalRatings(0);

        log.info("Creating new restaurant: {} with {} admins", restaurant.getName(), restaurant.getAdminIds().size());
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        // Update each cuisine type with the restaurant ID
        for (String cuisineTypeId : savedRestaurant.getCuisineTypeIds()) {
            CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

            if (cuisineType.getRestaurantIds() == null) {
                cuisineType.setRestaurantIds(new ArrayList<>());
            }

            if (!cuisineType.getRestaurantIds().contains(savedRestaurant.getId())) {
                cuisineType.getRestaurantIds().add(savedRestaurant.getId());
                cuisineType.setUpdatedAt(System.currentTimeMillis());
                cuisineTypeRepository.save(cuisineType);
            }
        }

        // Send notification event
        kafkaProducerService.sendRestaurantCreatedEvent(
                savedRestaurant.getId(),
                savedRestaurant.getName(),
                savedRestaurant.getEmail(),
                savedRestaurant.getPhoneNumber(),
                savedRestaurant.getAdminIds(),
                savedRestaurant.getCuisineTypeIds()
        );

        return savedRestaurant;
    }

    @Override
    public Restaurant updateRestaurant(String id, Restaurant restaurant, String token) {
        Restaurant existingRestaurant = getRestaurantById(id);
        String userId = restaurant.getAdminIds().isEmpty() ? null : restaurant.getAdminIds().getFirst();

        // Store original admin list to detect changes
        List<String> originalAdminIds = new ArrayList<>(existingRestaurant.getAdminIds());

        // Check token for ROLE_ADMIN - allow admins to update any restaurant
        boolean isAdmin = userServiceClient.validateUserRole(userId, "ROLE_ADMIN", token);

        // Only check restaurant ownership if not an admin
        if (!isAdmin && !existingRestaurant.getAdminIds().contains(userId)) {
            throw new BusinessValidationException("You don't have permission to update this restaurant");
        }

        validateRestaurantData(restaurant, userId, token);

        // Only allow admins to modify the admin users list
        if (!isAdmin) {
            // Non-admins must use the existing admin list
            restaurant.setAdminIds(existingRestaurant.getAdminIds());
        }

        // Update fields
        existingRestaurant.setName(restaurant.getName());
        existingRestaurant.setDescription(restaurant.getDescription());
        existingRestaurant.setAddress(restaurant.getAddress());
        existingRestaurant.setRestaurantImageUrl(restaurant.getRestaurantImageUrl());
        existingRestaurant.setBannerImageUrl(restaurant.getBannerImageUrl());
        existingRestaurant.setPhoneNumber(restaurant.getPhoneNumber());
        existingRestaurant.setEmail(restaurant.getEmail());
        existingRestaurant.setLatitude(restaurant.getLatitude());
        existingRestaurant.setLongitude(restaurant.getLongitude());
        existingRestaurant.setFormattedAddress(restaurant.getFormattedAddress());
        existingRestaurant.setOpeningHours(restaurant.getOpeningHours());
        existingRestaurant.setAdminIds(restaurant.getAdminIds());
        existingRestaurant.setEnabled(restaurant.isEnabled());
        existingRestaurant.setUpdatedAt(System.currentTimeMillis());

        // Handle cuisine type changes
        List<String> oldCuisineIds = existingRestaurant.getCuisineTypeIds();
        List<String> newCuisineIds = restaurant.getCuisineTypeIds();

        // Set new cuisine IDs
        existingRestaurant.setCuisineTypeIds(newCuisineIds);

        // Save restaurant first to ensure it exists
        Restaurant savedRestaurant = restaurantRepository.save(existingRestaurant);

        // Remove restaurant from cuisines that are no longer associated
        if (oldCuisineIds != null) {
            for (String cuisineId : oldCuisineIds) {
                if (!newCuisineIds.contains(cuisineId)) {
                    // This cuisine is no longer associated with this restaurant
                    CuisineType cuisineType = cuisineTypeRepository.findById(cuisineId)
                            .orElse(null);

                    if (cuisineType != null && cuisineType.getRestaurantIds() != null) {
                        cuisineType.getRestaurantIds().remove(savedRestaurant.getId());
                        cuisineType.setUpdatedAt(System.currentTimeMillis());
                        cuisineTypeRepository.save(cuisineType);
                    }
                }
            }
        }

        // Add restaurant to new cuisines
        if (newCuisineIds != null) {
            for (String cuisineId : newCuisineIds) {
                if (oldCuisineIds == null || !oldCuisineIds.contains(cuisineId)) {
                    // This is a newly associated cuisine
                    CuisineType cuisineType = cuisineTypeRepository.findById(cuisineId)
                            .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found: " + cuisineId));

                    if (cuisineType.getRestaurantIds() == null) {
                        cuisineType.setRestaurantIds(new ArrayList<>());
                    }

                    if (!cuisineType.getRestaurantIds().contains(savedRestaurant.getId())) {
                        cuisineType.getRestaurantIds().add(savedRestaurant.getId());
                        cuisineType.setUpdatedAt(System.currentTimeMillis());
                        cuisineTypeRepository.save(cuisineType);
                    }
                }
            }
        }

        List<String> addedAdmins = new ArrayList<>(savedRestaurant.getAdminIds());
        addedAdmins.removeAll(originalAdminIds);

        List<String> removedAdmins = new ArrayList<>(originalAdminIds);
        removedAdmins.removeAll(savedRestaurant.getAdminIds());

        // Send notification event with admin changes
        kafkaProducerService.sendRestaurantUpdatedEvent(
                savedRestaurant.getId(),
                savedRestaurant.getName(),
                savedRestaurant.getEmail(),
                savedRestaurant.getPhoneNumber(),
                savedRestaurant.getAdminIds(),
                addedAdmins.isEmpty() ? null : addedAdmins,
                removedAdmins.isEmpty() ? null : removedAdmins,
                savedRestaurant.getCuisineTypeIds()
        );

        log.info("Updated restaurant with id: {}", id);
        return savedRestaurant;
    }

    @Override
    public Restaurant getRestaurantById(String id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }

    @Override
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }

    @Override
    public void deleteRestaurant(String id, String userId, String token) {
        Restaurant restaurant = getRestaurantById(id);

        // Check if user is admin or restaurant admin
        boolean isAdmin = userServiceClient.validateUserRole(userId, "ROLE_ADMIN", token);
        if (!isAdmin && !restaurant.getAdminIds().contains(userId)) {
            throw new BusinessValidationException("You don't have permission to delete this restaurant");
        }

        log.info("Deleting restaurant with id: {}", id);
        restaurantRepository.delete(restaurant);
    }

    @Override
    public List<Restaurant> getRestaurantsByAdminId(String adminId) {
        return restaurantRepository.findByAdminIdsContaining(adminId);
    }

    @Override
    public Restaurant addAdminToRestaurant(String restaurantId, String adminId, String token) {
        Restaurant restaurant = getRestaurantById(restaurantId);

//        // Verify user exists
//        boolean isValidUser = userServiceClient.validateUserExists(adminId, token);
//        if (!isValidUser) {
//            throw new BusinessValidationException("Invalid user ID provided");
//        }

        // Verify user has correct role
        boolean isValidRole = userServiceClient.validateUserRole(adminId, "ROLE_RESTAURANT_ADMIN", token);
        if (!isValidRole) {
            throw new BusinessValidationException("User doesn't have required role to administer restaurants");
        }

        if (!restaurant.getAdminIds().contains(adminId)) {
            restaurant.getAdminIds().add(adminId);
            restaurant.setUpdatedAt(System.currentTimeMillis());
            return restaurantRepository.save(restaurant);
        }

        return restaurant;
    }

    @Override
    public Restaurant removeAdminFromRestaurant(String restaurantId, String adminId, String userId, String token) {
        Restaurant restaurant = getRestaurantById(restaurantId);

        // Check if user is admin or restaurant admin
        boolean isAdmin = userServiceClient.validateUserRole(userId, "ROLE_ADMIN", token);
        if (!isAdmin && !restaurant.getAdminIds().contains(userId)) {
            throw new BusinessValidationException("You don't have permission to remove admins from this restaurant");
        }

        // Can't remove the last admin
        if (restaurant.getAdminIds().size() <= 1 && restaurant.getAdminIds().contains(adminId)) {
            throw new BusinessValidationException("Cannot remove the last admin from restaurant");
        }

        if (restaurant.getAdminIds().contains(adminId)) {
            restaurant.getAdminIds().remove(adminId);
            restaurant.setUpdatedAt(System.currentTimeMillis());
            return restaurantRepository.save(restaurant);
        }

        return restaurant;
    }

    private void validateRestaurantData(Restaurant restaurant, String userId, String token) {
        if (userId == null || userId.isEmpty()) {
            throw new BusinessValidationException("User ID is required");
        }

        // Validate user has correct role
        boolean isValidRole = userServiceClient.validateUserRole(userId, "ROLE_RESTAURANT_ADMIN", token);
        if (!isValidRole) {
            throw new BusinessValidationException("User doesn't have required role to manage restaurants");
        }

        // Validate cuisine type IDs
        if (restaurant.getCuisineTypeIds() == null || restaurant.getCuisineTypeIds().isEmpty()) {
            throw new BusinessValidationException("At least one cuisine type must be selected");
        }

        List<CuisineType> foundCuisines = cuisineTypeRepository.findAllById(restaurant.getCuisineTypeIds());
        List<String> foundCuisineIds = foundCuisines.stream()
                .map(CuisineType::getId)
                .collect(Collectors.toList());

        if (foundCuisineIds.size() != restaurant.getCuisineTypeIds().size()) {
            List<String> invalidIds = new ArrayList<>(restaurant.getCuisineTypeIds());
            invalidIds.removeAll(foundCuisineIds);
            throw new BusinessValidationException("Invalid cuisine type IDs: " + String.join(", ", invalidIds));
        }

    }

    private void validateCuisineTypes(Restaurant restaurant) {
        if (restaurant.getCuisineTypeIds() == null || restaurant.getCuisineTypeIds().isEmpty()) {
            throw new BusinessValidationException("At least one cuisine type must be selected");
        }

        // Find all cuisines by ID
        List<CuisineType> foundCuisines = cuisineTypeRepository.findAllById(restaurant.getCuisineTypeIds());

        // Get list of valid cuisine IDs
        List<String> foundCuisineIds = foundCuisines.stream()
                .map(CuisineType::getId)
                .collect(Collectors.toList());

        // Check if all requested cuisine IDs exist
        if (foundCuisineIds.size() != restaurant.getCuisineTypeIds().size()) {
            List<String> invalidIds = new ArrayList<>(restaurant.getCuisineTypeIds());
            invalidIds.removeAll(foundCuisineIds);
            throw new BusinessValidationException("Invalid cuisine type IDs: " + String.join(", ", invalidIds));
        }

        // Check if all found cuisines are enabled
        List<CuisineType> disabledCuisines = foundCuisines.stream()
                .filter(cuisine -> !cuisine.isActive())
                .collect(Collectors.toList());

        if (!disabledCuisines.isEmpty()) {
            List<String> disabledIds = disabledCuisines.stream()
                    .map(CuisineType::getId)
                    .collect(Collectors.toList());
            throw new BusinessValidationException("Selected cuisine types are disabled: " + String.join(", ", disabledIds));
        }
    }
}