package com.foodDelivery.restaurantService.mapper;

import com.foodDelivery.restaurantService.dto.MenuCategoryResponse;
import com.foodDelivery.restaurantService.model.MenuCategory;

public class MenuCategoryTypeMapper {

    public static MenuCategoryResponse mapToResponse(MenuCategory entity) {
        MenuCategoryResponse response = new MenuCategoryResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setRestaurantId(entity.getRestaurantId());
        response.setDisplayOrder(entity.getDisplayOrder());
        response.setActive(entity.isActive());
        response.setMenuItemIds(entity.getMenuItemIds());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public MenuCategory mapToEntity(MenuCategoryResponse response) {
        MenuCategory entity = new MenuCategory();
        entity.setId(response.getId());
        entity.setName(response.getName());
        entity.setDescription(response.getDescription());
        entity.setRestaurantId(response.getRestaurantId());
        entity.setDisplayOrder(response.getDisplayOrder());
        entity.setActive(response.isActive());
        entity.setMenuItemIds(response.getMenuItemIds());
        entity.setCreatedAt(response.getCreatedAt());
        entity.setUpdatedAt(response.getUpdatedAt());
        return entity;
    }
}
