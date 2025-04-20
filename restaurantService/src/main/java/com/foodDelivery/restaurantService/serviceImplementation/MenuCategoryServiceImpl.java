package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.client.UserServiceClient;
import com.foodDelivery.restaurantService.dto.MenuCategoryRequest;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.model.MenuCategory;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.MenuCategoryRepository;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuCategoryServiceImpl implements MenuCategoryService {

    private final MenuCategoryRepository menuCategoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserServiceClient userServiceClient;

    @Override
    public MenuCategory createCategory(MenuCategoryRequest request, String token) {
        log.info("Creating menu category: {} for restaurant: {}", request.getName(), request.getRestaurantId());

        // Enhanced validation - check for input validity
        validateCategoryRequest(request);

        // Validate restaurant exists and is active
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + request.getRestaurantId()));

        if (!restaurant.isEnabled()) {
            throw new BusinessValidationException("Cannot add categories to inactive restaurant");
        }

        // Check if user has permission with enhanced validation
        validateUserPermission(restaurant, token);

        // Enhanced validation for duplicate names (case insensitive)
        if (menuCategoryRepository.existsByNameIgnoreCaseAndRestaurantId(
                request.getName().trim(), request.getRestaurantId())) {
            throw new BusinessValidationException("Category with name '" + request.getName() +
                    "' already exists for this restaurant");
        }

        // Check maximum category limit
        int categoryCount = menuCategoryRepository.countByRestaurantId(request.getRestaurantId());
        if (categoryCount >= 50) {
            throw new BusinessValidationException("Restaurant has reached maximum limit of 50 categories");
        }

        // Determine appropriate display order
        int displayOrder;
        if (request.getDisplayOrder() <= 0) {
            // Auto-assign next sequential order
            displayOrder = getNextDisplayOrder(request.getRestaurantId());
        } else {
            displayOrder = request.getDisplayOrder();
        }

        MenuCategory category = new MenuCategory();
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        category.setRestaurantId(request.getRestaurantId());
        category.setDisplayOrder(displayOrder);
        category.setActive(request.isActive());

        long currentTime = System.currentTimeMillis();
        category.setCreatedAt(currentTime);
        category.setUpdatedAt(currentTime);

        MenuCategory saved = menuCategoryRepository.save(category);
        log.debug("Created menu category with ID: {}", saved.getId());
        return saved;
    }

    private int getNextDisplayOrder(String restaurantId) {
        List<MenuCategory> existingCategories = menuCategoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);

        if (existingCategories.isEmpty()) {
            return 1; // First category starts at 1
        }

        // Find the maximum display order
        return existingCategories.stream()
                .mapToInt(MenuCategory::getDisplayOrder)
                .max()
                .orElse(0) + 1;
    }

    @Override
    public MenuCategory updateCategory(String id, MenuCategoryRequest request, String token) {
        log.info("Updating menu category: {}", id);

        // Find existing category
        MenuCategory existingCategory = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + id));

        // Find restaurant
        Restaurant restaurant = restaurantRepository.findById(existingCategory.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " +
                        existingCategory.getRestaurantId()));

        // Check if user has permission
        validateUserPermission(restaurant, token);

        // Check for duplicate name if name is changed
        if (!existingCategory.getName().equals(request.getName()) &&
                menuCategoryRepository.existsByNameAndRestaurantId(request.getName(), existingCategory.getRestaurantId())) {
            throw new BusinessValidationException("Category with name '" + request.getName() +
                    "' already exists for this restaurant");
        }

        // Update fields
        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());
        existingCategory.setDisplayOrder(request.getDisplayOrder());
        existingCategory.setActive(request.isActive());
        existingCategory.setUpdatedAt(System.currentTimeMillis());

        return menuCategoryRepository.save(existingCategory);
    }

    @Override
    public void deleteCategory(String id, String token) {
        log.info("Deleting menu category: {}", id);

        // Find existing category
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + id));

        // Store the category's display order and restaurant ID before deletion
        int deletedCategoryOrder = category.getDisplayOrder();
        String restaurantId = category.getRestaurantId();

        // Find restaurant
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        // Check if user has permission
        validateUserPermission(restaurant, token);

        // Check if category has menu items
        if (category.getMenuItemIds() != null && !category.getMenuItemIds().isEmpty()) {
            throw new BusinessValidationException(
                    "Cannot delete category that contains menu items. Remove all menu items first.");
        }

        // Delete the category
        menuCategoryRepository.deleteById(id);
        log.debug("Deleted menu category with ID: {}", id);

        // Update the order of remaining categories
        reorderCategoriesAfterDeletion(restaurantId, deletedCategoryOrder);
    }


    private void reorderCategoriesAfterDeletion(String restaurantId, int deletedOrder) {
        // Get all categories for this restaurant
        List<MenuCategory> remainingCategories = menuCategoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);

        // Update display order for categories that had a higher order than the deleted one
        long currentTime = System.currentTimeMillis();
        int updated = 0;

        for (MenuCategory category : remainingCategories) {
            if (category.getDisplayOrder() > deletedOrder) {
                category.setDisplayOrder(category.getDisplayOrder() - 1);
                category.setUpdatedAt(currentTime);
                menuCategoryRepository.save(category);
                updated++;
            }
        }

        log.debug("Updated display order for {} categories after deletion", updated);
    }

    @Override
    public MenuCategory getCategoryById(String id) {
        return menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + id));
    }

    @Override
    public List<MenuCategory> getCategoriesByRestaurantId(String restaurantId) {
        return menuCategoryRepository.findByRestaurantIdOrderByDisplayOrderAsc(restaurantId);
    }

    @Override
    public void reorderCategories(String restaurantId, List<String> categoryIds, String token) {
        log.info("Reordering menu categories for restaurant: {}", restaurantId);

        // Validate restaurant exists
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + restaurantId));

        // Check if user has permission
        validateUserPermission(restaurant, token);

        // Update display order for each category
        for (int i = 0; i < categoryIds.size(); i++) {
            String categoryId = categoryIds.get(i);
            MenuCategory category = menuCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + categoryId));

            // Validate category belongs to the restaurant
            if (!category.getRestaurantId().equals(restaurantId)) {
                throw new BusinessValidationException("Category " + categoryId + " does not belong to restaurant " + restaurantId);
            }

            category.setDisplayOrder(i+1); // Set display order starting from 1
            category.setUpdatedAt(System.currentTimeMillis());
            menuCategoryRepository.save(category);
        }
    }

    private void validateUserPermission(Restaurant restaurant, String token) {
        if (token == null || token.trim().isEmpty()) {
            log.error("Authorization token is missing or empty");
            throw new BusinessValidationException("Authorization required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        log.debug("Validating permissions for user: {}", currentUserId);

        // Check if current user is a system admin with enhanced error handling
        boolean isSystemAdmin;
        try {
            isSystemAdmin = userServiceClient.validateUserRole(currentUserId, "ROLE_ADMIN", token);
        } catch (Exception e) {
            log.error("Failed to validate admin role: {}", e.getMessage());
            throw new BusinessValidationException("User authorization service unavailable");
        }

        if (isSystemAdmin) {
            log.debug("User {} has system admin privileges", currentUserId);
            return; // Admin has full access
        }

        // Only check restaurant ownership if not an admin with improved error handling
        Long userID;
        try {
            userID = userServiceClient.getUserIdFromToken(token);
        } catch (Exception e) {
            log.error("Failed to extract user ID from token: {}", e.getMessage());
            throw new BusinessValidationException("Failed to validate user authorization");
        }

        if (userID == null) {
            log.error("User ID could not be extracted from token");
            throw new BusinessValidationException("Invalid authorization token");
        }

        // Validate restaurant admin role
        boolean isRestaurantAdmin = userServiceClient.validateUserRole(
                currentUserId, "ROLE_RESTAURANT_ADMIN", token);

        if (!isRestaurantAdmin) {
            log.warn("User {} is not a restaurant admin", currentUserId);
            throw new BusinessValidationException("You need restaurant administrator privileges");
        }

        // Check if user is admin for this specific restaurant
        if (!restaurant.getAdminIds().contains(String.valueOf(userID))) {
            log.warn("User {} is not an admin of restaurant {}", userID, restaurant.getId());
            throw new BusinessValidationException(
                    "You don't have administrator permissions for this restaurant");
        }
    }

    private void validateCategoryRequest(MenuCategoryRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessValidationException("Category name cannot be empty");
        }

        if (request.getName().trim().length() < 2 || request.getName().trim().length() > 50) {
            throw new BusinessValidationException("Category name must be between 2 and 50 characters");
        }

        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new BusinessValidationException("Category description cannot exceed 500 characters");
        }

        if (request.getDisplayOrder() < 0) {
            throw new BusinessValidationException("Display order cannot be negative");
        }
    }
}