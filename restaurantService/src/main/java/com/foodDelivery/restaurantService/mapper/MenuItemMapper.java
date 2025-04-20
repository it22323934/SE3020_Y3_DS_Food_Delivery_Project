package com.foodDelivery.restaurantService.mapper;

import com.foodDelivery.restaurantService.dto.MenuItemCreateRequest;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.model.MenuItem;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {
    public MenuItem toEntity(MenuItemCreateRequest request) {
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurantId(request.getRestaurantId());
        menuItem.setCategoryId(request.getCategoryId());
        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setPrice(request.getPrice());
        menuItem.setImageUrl(request.getImageUrl());
        menuItem.setAvailable(request.getAvailable());
        menuItem.setItemType(request.getItemType());
        menuItem.setVegetarian(request.getVegetarian());
        menuItem.setVegan(request.getVegan());
        menuItem.setGlutenFree(request.getGlutenFree());
        menuItem.setQuantity(request.getQuantity() != null ? request.getQuantity() : 0);
        menuItem.setExpiryDate(request.getExpiryDate());
        menuItem.setOnPromotion(request.getOnPromotion());
        menuItem.setDiscountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0);
        menuItem.setPreparationTimeMinutes(request.getPreparationTimeMinutes() != null ? request.getPreparationTimeMinutes() : 0);
        menuItem.setSpicy(request.getSpicy());
        return menuItem;
    }

    public MenuItemResponse toResponse(MenuItem menuItem) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(menuItem.getId());
        response.setRestaurantId(menuItem.getRestaurantId());
        response.setCategoryId(menuItem.getCategoryId());
        response.setName(menuItem.getName());
        response.setDescription(menuItem.getDescription());
        response.setPrice(menuItem.getPrice());
        response.setImageUrl(menuItem.getImageUrl());
        response.setAvailable(menuItem.isAvailable());
        response.setItemType(menuItem.getItemType());
        response.setVegetarian(menuItem.isVegetarian());
        response.setVegan(menuItem.isVegan());
        response.setGlutenFree(menuItem.isGlutenFree());
        response.setQuantity(menuItem.getQuantity());
        response.setExpiryDate(menuItem.getExpiryDate());
        response.setOnPromotion(menuItem.isOnPromotion());
        response.setDiscountPercentage(menuItem.getDiscountPercentage());
        response.setDiscountedPrice(menuItem.getDiscountedPrice());
        response.setPreparationTimeMinutes(menuItem.getPreparationTimeMinutes());
        response.setSpicy(menuItem.isSpicy());
        response.setPopularityScore(menuItem.getPopularityScore());
        response.setCreatedAt(menuItem.getCreatedAt());
        response.setUpdatedAt(menuItem.getUpdatedAt());
        return response;
    }
}