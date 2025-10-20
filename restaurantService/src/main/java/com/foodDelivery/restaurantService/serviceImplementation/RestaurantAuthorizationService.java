package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("restaurantAuthorizationService")
@RequiredArgsConstructor
@Slf4j
public class RestaurantAuthorizationService {

    private final RestaurantRepository restaurantRepository;

    /**
     * Check if the authenticated user can modify a specific restaurant
     * @param restaurantId Restaurant ID to check
     * @param authentication Current authenticated user
     * @return true if authorized, false otherwise
     */
    public boolean canModifyRestaurant(String restaurantId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Unauthorized access attempt to modify restaurant: {}", restaurantId);
            return false;
        }

        // Admin can modify all restaurants
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Restaurant admin can only modify their own restaurants
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_ADMIN"))) {

            Restaurant restaurant = restaurantRepository.findById(restaurantId).orElse(null);
            if (restaurant == null) {
                log.warn("Restaurant not found: {}", restaurantId);
                return false;
            }

            // Get authenticated user's username/email
            String authenticatedUser = authentication.getName();

            // Check if this user is the owner/admin of this restaurant
            boolean isOwner = restaurant.getAdminId() != null &&
                    restaurant.getAdminId().equals(authenticatedUser);

            if (!isOwner) {
                log.warn("User {} attempted to modify restaurant {} owned by {}",
                        authenticatedUser, restaurantId, restaurant.getAdminId());
            }

            return isOwner;
        }

        log.warn("User {} does not have permission to modify restaurants",
                authentication.getName());
        return false;
    }

    /**
     * Check if user can delete a restaurant
     * @param restaurantId Restaurant ID
     * @param authentication Current authenticated user
     * @return true if authorized, false otherwise
     */
    public boolean canDeleteRestaurant(String restaurantId, Authentication authentication) {
        // Same logic as modify - only owners and admins can delete
        return canModifyRestaurant(restaurantId, authentication);
    }

    /**
     * Check if user can access restaurant admin features
     * @param restaurantId Restaurant ID
     * @param authentication Current authenticated user
     * @return true if authorized, false otherwise
     */
    public boolean canAccessRestaurantAdmin(String restaurantId, Authentication authentication) {
        return canModifyRestaurant(restaurantId, authentication);
    }
}
