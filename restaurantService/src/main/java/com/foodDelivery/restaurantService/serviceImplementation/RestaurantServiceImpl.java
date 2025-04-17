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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        long userID = 0;
        Restaurant existingRestaurant = getRestaurantById(id);
        // Extract user ID from authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminUser = authentication.getName();

        // Store original admin list for notification
        List<String> originalAdminIds = new ArrayList<>(existingRestaurant.getAdminIds());

        // Check token for ROLE_ADMIN - allow admins to update any restaurant
        boolean isAdmin = userServiceClient.validateUserRole(adminUser, "ROLE_ADMIN", token);

        if(!isAdmin){
            userID = userServiceClient.getUserIdFromToken(token);
        }
        // Only check restaurant ownership if not an admin
        if (!isAdmin && !existingRestaurant.getAdminIds().contains(userID)) {
            throw new BusinessValidationException("You don't have permission to update this restaurant");
        }
        log.info("Cuisine ID: {}", restaurant.getCuisineTypeIds());
        validateCuisineTypes(restaurant);

        // Admin list handling
        if (!isAdmin) {
            // Non-admins cannot modify the admin list
            restaurant.setAdminIds(existingRestaurant.getAdminIds());
        } else if (restaurant.getAdminIds() != null) {
            // Only system admins can modify the admin list
            // Remove duplicates by converting to Set and back to List
            restaurant.setAdminIds(new ArrayList<>(new HashSet<>(restaurant.getAdminIds())));

            // Validate each admin has the correct role
            for (String adminId : restaurant.getAdminIds()) {
                if (!userServiceClient.validateUserRoleById(adminId, "ROLE_RESTAURANT_ADMIN", token)) {
                    throw new BusinessValidationException("User " + adminId +
                            " doesn't have required role to manage restaurants");
                }
            }

            // Ensure at least one admin remains
            if (restaurant.getAdminIds().isEmpty()) {
                throw new BusinessValidationException("Restaurant must have at least one admin");
            }
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
        existingRestaurant.setEnabled(restaurant.isEnabled());
        existingRestaurant.setUpdatedAt(System.currentTimeMillis());

        // Only update adminIds if it was changed
        if (restaurant.getAdminIds() != null) {
            existingRestaurant.setAdminIds(restaurant.getAdminIds());
        }

        // Handle cuisine type changes
        if (restaurant.getCuisineTypeIds() != null) {
            log.info("Processing cuisine types for restaurant: {}", id);

            // Ensure existing restaurant has initialized cuisine type list
            if (existingRestaurant.getCuisineTypeIds() == null) {
                existingRestaurant.setCuisineTypeIds(new ArrayList<>());
                log.info("Initialized empty cuisine type list for existing restaurant");
            }

            // Find cuisine types that were added and removed
            List<String> originalCuisineTypeIds = new ArrayList<>(existingRestaurant.getCuisineTypeIds());
            log.info("Original cuisine types: {}", originalCuisineTypeIds);
            log.info("New cuisine types: {}", restaurant.getCuisineTypeIds());

            List<String> addedCuisineTypes = new ArrayList<>(restaurant.getCuisineTypeIds());
            addedCuisineTypes.removeAll(originalCuisineTypeIds);
            log.info("Added cuisine types: {}", addedCuisineTypes);

            List<String> removedCuisineTypes = new ArrayList<>(originalCuisineTypeIds);
            removedCuisineTypes.removeAll(restaurant.getCuisineTypeIds());
            log.info("Removed cuisine types: {}", removedCuisineTypes);

            // Update each added cuisine type with this restaurant ID
            for (String cuisineTypeId : addedCuisineTypes) {
                try {
                    CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

                    if (cuisineType.getRestaurantIds() == null) {
                        cuisineType.setRestaurantIds(new ArrayList<>());
                    }

                    if (!cuisineType.getRestaurantIds().contains(id)) {
                        cuisineType.getRestaurantIds().add(id);
                        cuisineType.setUpdatedAt(System.currentTimeMillis());
                        cuisineTypeRepository.save(cuisineType);
                        log.info("Added restaurant {} to cuisine type {}", id, cuisineTypeId);
                    }
                } catch (Exception e) {
                    log.error("Error processing cuisine type {}: {}", cuisineTypeId, e.getMessage(), e);
                }
            }

            // Remove this restaurant ID from removed cuisine types
            for (String cuisineTypeId : removedCuisineTypes) {
                try {
                    CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

                    if (cuisineType.getRestaurantIds() != null && cuisineType.getRestaurantIds().contains(id)) {
                        cuisineType.getRestaurantIds().remove(id);
                        cuisineType.setUpdatedAt(System.currentTimeMillis());
                        cuisineTypeRepository.save(cuisineType);
                        log.info("Removed restaurant {} from cuisine type {}", id, cuisineTypeId);
                    }
                } catch (Exception e) {
                    log.error("Error removing cuisine type {}: {}", cuisineTypeId, e.getMessage(), e);
                }
            }

            // Update restaurant's cuisine type list
            existingRestaurant.setCuisineTypeIds(restaurant.getCuisineTypeIds());
        }

        // Calculate admin changes for notifications
        List<String> addedAdmins = new ArrayList<>(existingRestaurant.getAdminIds());
        addedAdmins.removeAll(originalAdminIds);

        List<String> removedAdmins = new ArrayList<>(originalAdminIds);
        removedAdmins.removeAll(existingRestaurant.getAdminIds());

        // Save restaurant and send notification
        Restaurant savedRestaurant = restaurantRepository.save(existingRestaurant);

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