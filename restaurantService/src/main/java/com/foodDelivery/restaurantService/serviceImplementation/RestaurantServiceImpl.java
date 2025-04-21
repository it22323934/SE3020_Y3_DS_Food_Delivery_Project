package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.client.UserServiceClient;
import com.foodDelivery.restaurantService.dto.RestaurantResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.mapper.RestaurantTypeMapper;
import com.foodDelivery.restaurantService.model.CuisineType;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.CuisineTypeRepository;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import com.foodDelivery.restaurantService.serviceInterfaces.KafkaProducerService;
import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
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
    private static final int MAX_CUISINE_TYPES_PER_RESTAURANT = 5;

    private static final String USER_SERVICE = "userService";

    @Override
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "createRestaurantFallback")
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

        return saveRestaurant(restaurant);
    }

    public Restaurant createRestaurantFallback(Restaurant restaurant, String token, Exception e) {
        log.warn("User service is down. Using fallback for restaurant creation with limited validation: {}", e.getMessage());

        // Extract user ID from authentication to use as admin
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();

        // In fallback mode, just assign the current user as admin
        restaurant.setAdminIds(List.of(currentUserId));

        return saveRestaurant(restaurant);
    }

    private Restaurant saveRestaurant(Restaurant restaurant) {
        // Check for duplicate restaurant name
        if (restaurant.getName() != null && restaurantRepository.existsByName(restaurant.getName())) {
            throw new BusinessValidationException("Restaurant with name '" + restaurant.getName() + "' already exists");
        }

        // Check for duplicate email if provided
        if (restaurant.getEmail() != null && !restaurant.getEmail().isEmpty() &&
                restaurantRepository.existsByEmail(restaurant.getEmail())) {
            throw new BusinessValidationException("Restaurant with email '" + restaurant.getEmail() + "' already exists");
        }

        // Check for duplicate phone number if provided
        if (restaurant.getPhoneNumber() != null && !restaurant.getPhoneNumber().isEmpty() &&
                restaurantRepository.existsByPhoneNumber(restaurant.getPhoneNumber())) {
            throw new BusinessValidationException("Restaurant with phone number '" + restaurant.getPhoneNumber() + "' already exists");
        }

        // Validate other restaurant data like cuisine types
        validateCuisineTypes(restaurant);

        long currentTime = System.currentTimeMillis();
        restaurant.setCreatedAt(currentTime);
        restaurant.setUpdatedAt(currentTime);
        restaurant.setEnabled(true);
        restaurant.setAvgRating(0.0);
        restaurant.setTotalRatings(0);
        if (restaurant.getLatitude() != null && restaurant.getLongitude() != null) {
            double[] location = new double[] {restaurant.getLongitude(), restaurant.getLatitude()};
            restaurant.setLocation(location);
        }

        log.info("Creating new restaurant: {} with {} admins", restaurant.getName(), restaurant.getAdminIds().size());
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        // Update each cuisine type with the restaurant ID
        updateCuisineTypesWithRestaurant(savedRestaurant);

        // Send notification event
        sendRestaurantCreatedNotification(savedRestaurant);

        return savedRestaurant;
    }

    private void updateCuisineTypesWithRestaurant(Restaurant restaurant) {
        for (String cuisineTypeId : restaurant.getCuisineTypeIds()) {
            try {
                CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

                if (cuisineType.getRestaurantIds() == null) {
                    cuisineType.setRestaurantIds(new ArrayList<>());
                }

                if (!cuisineType.getRestaurantIds().contains(restaurant.getId())) {
                    cuisineType.getRestaurantIds().add(restaurant.getId());
                    cuisineType.setUpdatedAt(System.currentTimeMillis());
                    cuisineTypeRepository.save(cuisineType);
                }
            } catch (Exception e) {
                log.error("Error updating cuisine type {}: {}", cuisineTypeId, e.getMessage());
            }
        }
    }

    private void sendRestaurantCreatedNotification(Restaurant restaurant) {
        try {
            kafkaProducerService.sendRestaurantCreatedEvent(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getEmail(),
                    restaurant.getPhoneNumber(),
                    restaurant.getAdminIds(),
                    restaurant.getCuisineTypeIds()
            );
        } catch (Exception e) {
            log.error("Failed to send restaurant creation notification: {}", e.getMessage());
            // Continue execution - notification failure shouldn't stop restaurant creation
        }
    }

    @Override
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "updateRestaurantFallback")
    public Restaurant updateRestaurant(String id, Restaurant restaurant, String token) {
        // Extract user ID from authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminUser = authentication.getName();

        Restaurant existingRestaurant = getRestaurantById(id);
        List<String> originalAdminIds = new ArrayList<>(existingRestaurant.getAdminIds());

        // Check if current user is a system admin
        boolean isAdmin = userServiceClient.validateUserRole(adminUser, "ROLE_ADMIN", token);

        // Verify permissions
        if (!isAdmin) {
            Long userID = userServiceClient.getUserIdFromToken(token);
            if (userID == null) {
                throw new BusinessValidationException("Unable to validate user ID from token");
            }

            // Only check restaurant ownership if not an admin
            if (!existingRestaurant.getAdminIds().contains(String.valueOf(userID))) {
                throw new BusinessValidationException("You don't have permission to update this restaurant");
            }
        }

        return processRestaurantUpdate(id, restaurant, existingRestaurant, originalAdminIds, isAdmin, token);
    }

    public Restaurant updateRestaurantFallback(String id, Restaurant restaurant, String token, Exception e) {
        log.warn("User service is down. Using fallback for restaurant update with limited validation: {}", e.getMessage());

        // Extract user ID from authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminUser = authentication.getName();

        Restaurant existingRestaurant = getRestaurantById(id);
        List<String> originalAdminIds = new ArrayList<>(existingRestaurant.getAdminIds());

        // In fallback mode, only allow updates if user is already in admin list
        if (!existingRestaurant.getAdminIds().contains(adminUser)) {
            throw new BusinessValidationException("Cannot verify permissions while user service is unavailable");
        }

        // Don't allow admin list modification in fallback mode
        restaurant.setAdminIds(existingRestaurant.getAdminIds());

        return processRestaurantUpdate(id, restaurant, existingRestaurant, originalAdminIds, false, token);
    }

    private Restaurant processRestaurantUpdate(String id, Restaurant restaurant,
                                               Restaurant existingRestaurant,
                                               List<String> originalAdminIds,
                                               boolean isAdmin,
                                               String token) {
        // Validate cuisine types
        validateCuisineTypes(restaurant);

        // Handle admin list updates
        processAdminListUpdates(restaurant, existingRestaurant, isAdmin, token);

        // Validate against duplicates
        checkForDuplicateDetails(restaurant, existingRestaurant);

        // Update basic fields
        updateBasicFields(restaurant, existingRestaurant);

        // Process cuisine type changes
        processCuisineTypeChanges(id, restaurant, existingRestaurant);

        // Calculate admin changes for notifications
        List<String> addedAdmins = calculateAddedAdmins(existingRestaurant.getAdminIds(), originalAdminIds);
        List<String> removedAdmins = calculateRemovedAdmins(existingRestaurant.getAdminIds(), originalAdminIds);

        // Save restaurant
        Restaurant savedRestaurant = restaurantRepository.save(existingRestaurant);

        // Send notification event
        sendRestaurantUpdateNotification(savedRestaurant, addedAdmins, removedAdmins);

        log.info("Updated restaurant with id: {}", id);
        return savedRestaurant;
    }

    private void processAdminListUpdates(Restaurant restaurant, Restaurant existingRestaurant, boolean isAdmin, String token) {
        if (!isAdmin) {
            // Non-admins cannot modify the admin list
            restaurant.setAdminIds(existingRestaurant.getAdminIds());
        } else if (restaurant.getAdminIds() != null) {
            // Remove duplicates
            restaurant.setAdminIds(new ArrayList<>(new HashSet<>(restaurant.getAdminIds())));

            try {
                // Validate each admin has the correct role if user service is available
                for (String adminId : restaurant.getAdminIds()) {
                    if (!userServiceClient.validateUserRoleById(adminId, "ROLE_RESTAURANT_ADMIN", token)) {
                        throw new BusinessValidationException("User " + adminId +
                                " doesn't have required role to manage restaurants");
                    }
                }
            } catch (Exception e) {
                log.warn("Could not validate admin roles - user service may be unavailable: {}", e.getMessage());
                // Continue with updates but log warning
            }

            // Ensure at least one admin remains
            if (restaurant.getAdminIds().isEmpty()) {
                throw new BusinessValidationException("Restaurant must have at least one admin");
            }

            // Validate that admins are not assigned to other restaurants
            validateUniqueAdmins(restaurant.getAdminIds(), existingRestaurant.getId());

        }
    }

    private void checkForDuplicateDetails(Restaurant restaurant, Restaurant existingRestaurant) {
        // Check for duplicate restaurant name if changed
        if (restaurant.getName() != null && !restaurant.getName().equals(existingRestaurant.getName()) &&
                restaurantRepository.existsByName(restaurant.getName())) {
            throw new BusinessValidationException("Restaurant with name '" + restaurant.getName() + "' already exists");
        }

        // Check for duplicate email if changed and provided
        if (restaurant.getEmail() != null && !restaurant.getEmail().isEmpty() &&
                !restaurant.getEmail().equals(existingRestaurant.getEmail()) &&
                restaurantRepository.existsByEmail(restaurant.getEmail())) {
            throw new BusinessValidationException("Restaurant with email '" + restaurant.getEmail() + "' already exists");
        }

        // Check for duplicate phone number if changed and provided
        if (restaurant.getPhoneNumber() != null && !restaurant.getPhoneNumber().isEmpty() &&
                !restaurant.getPhoneNumber().equals(existingRestaurant.getPhoneNumber()) &&
                restaurantRepository.existsByPhoneNumber(restaurant.getPhoneNumber())) {
            throw new BusinessValidationException("Restaurant with phone number '" + restaurant.getPhoneNumber() + "' already exists");
        }
    }

    private void updateBasicFields(Restaurant restaurant, Restaurant existingRestaurant) {
        existingRestaurant.setName(restaurant.getName());
        existingRestaurant.setDescription(restaurant.getDescription());
        existingRestaurant.setAddress(restaurant.getAddress());
        existingRestaurant.setRestaurantImageUrl(restaurant.getRestaurantImageUrl());
        existingRestaurant.setBannerImageUrl(restaurant.getBannerImageUrl());
        existingRestaurant.setPhoneNumber(restaurant.getPhoneNumber());
        existingRestaurant.setEmail(restaurant.getEmail());
        double[] location = new double[] {restaurant.getLongitude(), restaurant.getLatitude()};
        existingRestaurant.setLocation(location);
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
    }

    private void processCuisineTypeChanges(String id, Restaurant restaurant, Restaurant existingRestaurant) {
        if (restaurant.getCuisineTypeIds() != null) {
            // Ensure existing restaurant has initialized cuisine type list
            if (existingRestaurant.getCuisineTypeIds() == null) {
                existingRestaurant.setCuisineTypeIds(new ArrayList<>());
            }

            // Find cuisine types that were added and removed
            List<String> originalCuisineTypeIds = new ArrayList<>(existingRestaurant.getCuisineTypeIds());

            List<String> addedCuisineTypes = new ArrayList<>(restaurant.getCuisineTypeIds());
            addedCuisineTypes.removeAll(originalCuisineTypeIds);

            List<String> removedCuisineTypes = new ArrayList<>(originalCuisineTypeIds);
            removedCuisineTypes.removeAll(restaurant.getCuisineTypeIds());

            // Update each added cuisine type with this restaurant ID
            processCuisineTypeAdditions(id, addedCuisineTypes);

            // Remove this restaurant ID from removed cuisine types
            processCuisineTypeRemovals(id, removedCuisineTypes);

            // Update restaurant's cuisine type list
            existingRestaurant.setCuisineTypeIds(restaurant.getCuisineTypeIds());
        }
    }

    private void processCuisineTypeAdditions(String restaurantId, List<String> addedCuisineTypes) {
        for (String cuisineTypeId : addedCuisineTypes) {
            try {
                CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

                if (cuisineType.getRestaurantIds() == null) {
                    cuisineType.setRestaurantIds(new ArrayList<>());
                }

                if (!cuisineType.getRestaurantIds().contains(restaurantId)) {
                    cuisineType.getRestaurantIds().add(restaurantId);
                    cuisineType.setUpdatedAt(System.currentTimeMillis());
                    cuisineTypeRepository.save(cuisineType);
                }
            } catch (Exception e) {
                log.error("Error processing cuisine type addition {}: {}", cuisineTypeId, e.getMessage());
            }
        }
    }

    private void processCuisineTypeRemovals(String restaurantId, List<String> removedCuisineTypes) {
        for (String cuisineTypeId : removedCuisineTypes) {
            try {
                CuisineType cuisineType = cuisineTypeRepository.findById(cuisineTypeId)
                        .orElseThrow(() -> new ResourceNotFoundException("Cuisine type not found with id: " + cuisineTypeId));

                if (cuisineType.getRestaurantIds() != null && cuisineType.getRestaurantIds().contains(restaurantId)) {
                    cuisineType.getRestaurantIds().remove(restaurantId);
                    cuisineType.setUpdatedAt(System.currentTimeMillis());
                    cuisineTypeRepository.save(cuisineType);
                }
            } catch (Exception e) {
                log.error("Error processing cuisine type removal {}: {}", cuisineTypeId, e.getMessage());
            }
        }
    }

    private List<String> calculateAddedAdmins(List<String> currentAdmins, List<String> originalAdmins) {
        List<String> addedAdmins = new ArrayList<>(currentAdmins);
        addedAdmins.removeAll(originalAdmins);
        return addedAdmins;
    }

    private List<String> calculateRemovedAdmins(List<String> currentAdmins, List<String> originalAdmins) {
        List<String> removedAdmins = new ArrayList<>(originalAdmins);
        removedAdmins.removeAll(currentAdmins);
        return removedAdmins;
    }

    private void sendRestaurantUpdateNotification(Restaurant restaurant, List<String> addedAdmins, List<String> removedAdmins) {
        try {
            kafkaProducerService.sendRestaurantUpdatedEvent(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getEmail(),
                    restaurant.getPhoneNumber(),
                    restaurant.getAdminIds(),
                    addedAdmins.isEmpty() ? null : addedAdmins,
                    removedAdmins.isEmpty() ? null : removedAdmins,
                    restaurant.getCuisineTypeIds()
            );
        } catch (Exception e) {
            log.error("Failed to send restaurant update notification: {}", e.getMessage());
            // Continue execution - notification failure shouldn't stop restaurant update
        }
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
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "deleteRestaurantFallback")
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

    public void deleteRestaurantFallback(String id, String userId, String token, Exception e) {
        log.warn("User service is down. Using fallback for restaurant deletion: {}", e.getMessage());

        Restaurant restaurant = getRestaurantById(id);

        // In fallback mode, only allow deletion if user is in admin list
        if (restaurant.getAdminIds().contains(userId)) {
            log.info("Deleting restaurant with id: {} in fallback mode", id);
            restaurantRepository.delete(restaurant);
        } else {
            throw new BusinessValidationException("Cannot verify permissions while user service is unavailable");
        }
    }

    @Override
    public List<Restaurant> getRestaurantsByAdminId(String adminId) {
        return restaurantRepository.findByAdminIdsContaining(adminId);
    }

    @Override
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "addAdminToRestaurantFallback")
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

    public Restaurant addAdminToRestaurantFallback(String restaurantId, String adminId, String token, Exception e) {
        log.warn("User service is down. Cannot add admin to restaurant: {}", e.getMessage());
        throw new BusinessValidationException("Cannot add admin while user service is unavailable");
    }

    @Override
    @CircuitBreaker(name = USER_SERVICE, fallbackMethod = "removeAdminFromRestaurantFallback")
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

    public Restaurant removeAdminFromRestaurantFallback(String restaurantId, String adminId, String userId, String token, Exception e) {
        log.warn("User service is down. Using fallback for admin removal: {}", e.getMessage());

        Restaurant restaurant = getRestaurantById(restaurantId);

        // In fallback mode, only allow removal if current user is in admin list
        if (!restaurant.getAdminIds().contains(userId)) {
            throw new BusinessValidationException("Cannot verify permissions while user service is unavailable");
        }

        // Can't remove the last admin
        if (restaurant.getAdminIds().size() <= 1) {
            throw new BusinessValidationException("Cannot remove the last admin from restaurant");
        }

        // Can't remove yourself in fallback mode
        if (adminId.equals(userId)) {
            throw new BusinessValidationException("Cannot remove yourself as admin in fallback mode");
        }

        if (restaurant.getAdminIds().contains(adminId)) {
            restaurant.getAdminIds().remove(adminId);
            restaurant.setUpdatedAt(System.currentTimeMillis());
            return restaurantRepository.save(restaurant);
        }

        return restaurant;
    }

    private void validateUniqueAdmins(List<String> adminIds, String currentRestaurantId) {
        if (adminIds == null || adminIds.isEmpty()) {
            return;
        }

        for (String adminId : adminIds) {
            List<Restaurant> restaurants = restaurantRepository.findByAdminIdsContaining(adminId);

            for (Restaurant restaurant : restaurants) {
                // Skip if it's the current restaurant we're updating
                if (currentRestaurantId != null && restaurant.getId().equals(currentRestaurantId)) {
                    continue;
                }

                throw new BusinessValidationException(
                        "User with ID " + adminId + " is already an admin for restaurant '" +
                                restaurant.getName() + "'. An admin can only manage one restaurant."
                );
            }
        }
    }

    private void validateCuisineTypes(Restaurant restaurant) {
        if (restaurant.getCuisineTypeIds() == null || restaurant.getCuisineTypeIds().isEmpty()) {
            throw new BusinessValidationException("At least one cuisine type must be selected");
        }

        // Check if number of cuisine types exceeds limit
        if (restaurant.getCuisineTypeIds().size() > MAX_CUISINE_TYPES_PER_RESTAURANT) {
            throw new BusinessValidationException("Restaurant cannot have more than " +
                    MAX_CUISINE_TYPES_PER_RESTAURANT + " cuisine types");
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

    @Override
    public List<RestaurantResponse> getNearbyRestaurants(double latitude, double longitude, double radius) {
        // Validate inputs
        if (latitude < -90 || latitude > 90) {
            throw new BusinessValidationException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude < -180 || longitude > 180) {
            throw new BusinessValidationException("Longitude must be between -180 and 180 degrees");
        }
        if (radius <= 0 || radius > 50) {
            throw new BusinessValidationException("Radius must be positive and not more than 50 km");
        }

        // Convert radius from km to meters if using MongoDB
        double radiusInMeters = radius * 1000;

        // Find nearby restaurants
        List<Restaurant> restaurants = restaurantRepository.findNearbyRestaurants(
                latitude, longitude, radiusInMeters);

        return restaurants.stream()
                .map(RestaurantTypeMapper::mapToResponse)
                .collect(Collectors.toList());
    }
}