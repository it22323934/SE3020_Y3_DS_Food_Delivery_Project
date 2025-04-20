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

import java.util.List;
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

        // Create menu item
        MenuItem menuItem = menuItemMapper.toEntity(request);
        long now = System.currentTimeMillis();
        menuItem.setCreatedAt(now);
        menuItem.setUpdatedAt(now);

        // Calculate discounted price if applicable
        if (Boolean.TRUE.equals(menuItem.isOnPromotion()) && menuItem.getDiscountPercentage() > 0) {
            double discountedPrice = menuItem.getPrice() * (1 - (menuItem.getDiscountPercentage() / 100));
            menuItem.setDiscountedPrice(discountedPrice);
        } else {
            menuItem.setDiscountedPrice(menuItem.getPrice());
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
    public MenuItemResponse updateMenuItem(String id, MenuItemCreateRequest request, String token) {
        // Implementation for update method would go here
        // This is a placeholder for future implementation
        return null;
    }

    @Override
    public void deleteMenuItem(String id, String token) {
        // Implementation for delete method would go here
        // This is a placeholder for future implementation
    }
}