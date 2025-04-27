package com.foodDelivery.restaurantService.serviceInterfaces;

import com.foodDelivery.restaurantService.dto.MenuCategoryRequest;
import com.foodDelivery.restaurantService.model.MenuCategory;

import java.util.List;

public interface MenuCategoryService {
    MenuCategory createCategory(MenuCategoryRequest request, String token);
    MenuCategory updateCategory(String id, MenuCategoryRequest request, String token);
    void deleteCategory(String id, String token);
    MenuCategory getCategoryById(String id);
    List<MenuCategory> getCategoriesByRestaurantId(String restaurantId);
    void reorderCategories(String restaurantId, List<String> categoryIds, String token);
    List<MenuCategory> getActiveCategoriesByRestaurantId(String restaurantId);
}