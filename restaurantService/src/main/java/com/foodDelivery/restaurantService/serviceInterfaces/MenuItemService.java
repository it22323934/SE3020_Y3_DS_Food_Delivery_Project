package com.foodDelivery.restaurantService.serviceInterfaces;

import com.foodDelivery.restaurantService.dto.MenuItemCreateRequest;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;

import java.util.List;

public interface MenuItemService {
    MenuItemResponse createMenuItem(MenuItemCreateRequest request, String token);
    MenuItemResponse getMenuItemById(String id);
    List<MenuItemResponse> getMenuItemsByRestaurantId(String restaurantId);
    List<MenuItemResponse> getMenuItemsByCategoryId(String categoryId);
    MenuItemResponse updateMenuItem(String id, MenuItemCreateRequest request, String token);
    void deleteMenuItem(String id);
}