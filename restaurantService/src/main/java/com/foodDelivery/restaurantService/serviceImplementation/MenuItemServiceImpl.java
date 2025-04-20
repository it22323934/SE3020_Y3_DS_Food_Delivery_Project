package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.dto.MenuItemCreateRequest;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.mapper.MenuItemMapper;
import com.foodDelivery.restaurantService.model.MenuCategory;
import com.foodDelivery.restaurantService.model.MenuItem;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.MenuCategoryRepository;
import com.foodDelivery.restaurantService.repository.MenuItemRepository;
import com.foodDelivery.restaurantService.repository.RestaurantRepository;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemMapper menuItemMapper;

    @Override
    @Transactional
    public MenuItemResponse createMenuItem(MenuItemCreateRequest request, String token) {
        // Validate restaurant exists
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + request.getRestaurantId()));

        // Validate menu category exists
        MenuCategory category = menuCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + request.getCategoryId()));

        // Validate category belongs to the restaurant
        if (!category.getRestaurantId().equals(request.getRestaurantId())) {
            throw new BusinessValidationException("Category does not belong to the restaurant");
        }

        // Validate no duplicate menu item name for the same restaurant
        if (menuItemRepository.existsByRestaurantIdAndNameIgnoreCase(request.getRestaurantId(), request.getName())) {
            throw new BusinessValidationException("Menu item with name '" + request.getName() + "' already exists for this restaurant");
        }

        // Validate vegetarian/non-vegetarian flags
        if (Boolean.TRUE.equals(request.getVegetarian()) && Boolean.TRUE.equals(request.getNonVegetarian())) {
            throw new BusinessValidationException("A menu item cannot be both vegetarian and non-vegetarian");
        }

        // Validate unit if provided
        if (request.getUnit() != null && !request.getUnit().isEmpty()) {
            List<String> validUnits = List.of("ITEMS", "KG", "G", "L", "ML", "PACK", "BOX");
            if (!validUnits.contains(request.getUnit().toUpperCase())) {
                throw new BusinessValidationException("Invalid unit type. Accepted values are: " +
                        String.join(", ", validUnits));
            }
            // Normalize the unit to uppercase
            request.setUnit(request.getUnit().toUpperCase());
        }

        // Validate add-ons
        validateAddOns(request.getAddOns());

        // Create menu item
        MenuItem menuItem = menuItemMapper.toEntity(request);
        long now = System.currentTimeMillis();
        menuItem.setCreatedAt(now);
        menuItem.setUpdatedAt(now);

        // Calculate discounted price if applicable
        if (Boolean.TRUE.equals(menuItem.isOnPromotion())) {
            // If client provided discountedPrice, calculate the percentage
            if (request.getDiscountedPrice() != null && request.getDiscountedPrice() < menuItem.getPrice()) {
                double discountPercentage = 100 * (1 - (request.getDiscountedPrice() / menuItem.getPrice()));
                menuItem.setDiscountPercentage(discountPercentage);
                menuItem.setDiscountedPrice(request.getDiscountedPrice());
            }
            // If client provided percentage, calculate the discounted price
            else if (menuItem.getDiscountPercentage() > 0) {
                double discountedPrice = menuItem.getPrice() * (1 - (menuItem.getDiscountPercentage() / 100));
                menuItem.setDiscountedPrice(discountedPrice);
            }
            // If neither is provided, throw error
            else {
                throw new BusinessValidationException("Items on promotion must have either a discounted price or discount percentage");
            }
        } else {
            menuItem.setDiscountedPrice(menuItem.getPrice());
            menuItem.setDiscountPercentage(0.0);
        }

        MenuItem saved = menuItemRepository.save(menuItem);

        // Add menu item to category
        if (!category.getMenuItemIds().contains(saved.getId())) {
            category.getMenuItemIds().add(saved.getId());
            category.setUpdatedAt(now);
            menuCategoryRepository.save(category);
        }

        return menuItemMapper.toResponse(saved);
    }

    private void validateAddOns(List<MenuItemCreateRequest.AddOnRequest> addOns) {
        if (addOns == null || addOns.isEmpty()) {
            return;
        }

        // Check for duplicate add-on names
        Set<String> addOnNames = new HashSet<>();
        for (MenuItemCreateRequest.AddOnRequest addOn : addOns) {
            if (addOn.getName() == null || addOn.getName().trim().isEmpty()) {
                throw new BusinessValidationException("Add-on name cannot be empty");
            }

            String normalizedName = addOn.getName().toLowerCase().trim();
            if (!addOnNames.add(normalizedName)) {
                throw new BusinessValidationException("Duplicate add-on name: " + addOn.getName());
            }

            // Validate price is non-negative
            if (addOn.getPrice() == null || addOn.getPrice() < 0) {
                throw new BusinessValidationException("Add-on price cannot be negative for: " + addOn.getName());
            }

            // Validate max quantity
            if (addOn.getMaxQuantity() != null && addOn.getMaxQuantity() < 1) {
                throw new BusinessValidationException("Add-on max quantity must be at least 1 for: " + addOn.getName());
            }
        }
    }

    @Override
    public MenuItemResponse getMenuItemById(String id) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
        return menuItemMapper.toResponse(menuItem);
    }

    @Override
    public List<MenuItemResponse> getMenuItemsByRestaurantId(String restaurantId) {
        // Validate restaurant exists
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + restaurantId);
        }

        List<MenuItem> menuItems = menuItemRepository.findByRestaurantId(restaurantId);
        return menuItems.stream()
                .map(menuItemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MenuItemResponse> getMenuItemsByCategoryId(String categoryId) {
        // Validate category exists
        if (!menuCategoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Menu category not found with id: " + categoryId);
        }

        List<MenuItem> menuItems = menuItemRepository.findByCategoryId(categoryId);
        return menuItems.stream()
                .map(menuItemMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(String id, MenuItemCreateRequest request, String token) {
        // Fetch the existing menu item
        MenuItem existingMenuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));

        // Validate restaurant exists
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + request.getRestaurantId()));

        // Validate category exists
        MenuCategory newCategory = menuCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + request.getCategoryId()));

        // Validate new category belongs to the restaurant
        if (!newCategory.getRestaurantId().equals(request.getRestaurantId())) {
            throw new BusinessValidationException("Category does not belong to the restaurant");
        }

        // Check for name uniqueness if name is changed
        if (!existingMenuItem.getName().equalsIgnoreCase(request.getName()) &&
                menuItemRepository.existsByRestaurantIdAndNameIgnoreCase(request.getRestaurantId(), request.getName())) {
            throw new BusinessValidationException("Menu item with name '" + request.getName() + "' already exists");
        }

        // Field validations
        validateMenuItemFields(request);

        // Handle category change if necessary
        boolean categoryChanged = !existingMenuItem.getCategoryId().equals(request.getCategoryId());
        if (categoryChanged) {
            // Get the old category
            MenuCategory oldCategory = menuCategoryRepository.findById(existingMenuItem.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Old menu category not found"));

            // Remove menu item from old category
            oldCategory.getMenuItemIds().remove(id);
            oldCategory.setUpdatedAt(System.currentTimeMillis());
            menuCategoryRepository.save(oldCategory);

            // Add menu item to new category
            if (!newCategory.getMenuItemIds().contains(id)) {
                newCategory.getMenuItemIds().add(id);
                newCategory.setUpdatedAt(System.currentTimeMillis());
                menuCategoryRepository.save(newCategory);
            }
        }

        // Update the menu item
        MenuItem updatedMenuItem = menuItemMapper.toEntity(request);
        updatedMenuItem.setId(id);
        updatedMenuItem.setCreatedAt(existingMenuItem.getCreatedAt());
        updatedMenuItem.setUpdatedAt(System.currentTimeMillis());

        // Handle discount calculation
        if (Boolean.TRUE.equals(updatedMenuItem.isOnPromotion())) {
            // If client provided discountedPrice, calculate the percentage
            if (request.getDiscountedPrice() != null && request.getDiscountedPrice() < updatedMenuItem.getPrice()) {
                double discountPercentage = 100 * (1 - (request.getDiscountedPrice() / updatedMenuItem.getPrice()));
                updatedMenuItem.setDiscountPercentage(discountPercentage);
                updatedMenuItem.setDiscountedPrice(request.getDiscountedPrice());
            }
            // If client provided percentage, calculate the discounted price
            else if (updatedMenuItem.getDiscountPercentage() > 0) {
                double discountedPrice = updatedMenuItem.getPrice() * (1 - (updatedMenuItem.getDiscountPercentage() / 100));
                updatedMenuItem.setDiscountedPrice(discountedPrice);
            }
            // If neither is provided, throw error
            else {
                throw new BusinessValidationException("Items on promotion must have either a discounted price or discount percentage");
            }
        } else {
            updatedMenuItem.setDiscountedPrice(updatedMenuItem.getPrice());
            updatedMenuItem.setDiscountPercentage(0.0);
        }

        MenuItem saved = menuItemRepository.save(updatedMenuItem);
        log.info("Menu item updated: {}", id);

        return menuItemMapper.toResponse(saved);
    }

    private void validateMenuItemFields(MenuItemCreateRequest request) {
        // Validate vegetarian/non-vegetarian flags
        if (Boolean.TRUE.equals(request.getVegetarian()) && Boolean.TRUE.equals(request.getNonVegetarian())) {
            throw new BusinessValidationException("A menu item cannot be both vegetarian and non-vegetarian");
        }

        // Validate unit if provided
        if (request.getUnit() != null && !request.getUnit().isEmpty()) {
            List<String> validUnits = List.of("ITEMS", "KG", "G", "L", "ML", "PACK", "BOX");
            if (!validUnits.contains(request.getUnit().toUpperCase())) {
                throw new BusinessValidationException("Invalid unit type. Accepted values are: " +
                        String.join(", ", validUnits));
            }
            request.setUnit(request.getUnit().toUpperCase());
        }

        // Validate price isn't negative
        if (request.getPrice() != null && request.getPrice() < 0) {
            throw new BusinessValidationException("Price cannot be negative");
        }

        // Validate quantity isn't negative
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new BusinessValidationException("Quantity cannot be negative");
        }

        // Validate discounted price isn't higher than regular price
        if (request.getDiscountedPrice() != null && request.getPrice() != null &&
                request.getDiscountedPrice() > request.getPrice()) {
            throw new BusinessValidationException("Discounted price cannot be higher than regular price");
        }

        // Validate add-ons
        validateAddOns(request.getAddOns());
    }

    @Override
    @Transactional
    public void deleteMenuItem(String id) {
        // Validate menu item exists
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));

        // Find the category that contains this menu item
        MenuCategory category = menuCategoryRepository.findById(menuItem.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found with id: " + menuItem.getCategoryId()));

        // Remove the menu item ID from the category's menuItemIds list
        category.getMenuItemIds().remove(id);

        // Update the category's updatedAt timestamp
        category.setUpdatedAt(System.currentTimeMillis());

        // Save the updated category
        menuCategoryRepository.save(category);

        // Delete the menu item
        menuItemRepository.deleteById(id);

        log.info("Menu item deleted: {}", id);
    }
}