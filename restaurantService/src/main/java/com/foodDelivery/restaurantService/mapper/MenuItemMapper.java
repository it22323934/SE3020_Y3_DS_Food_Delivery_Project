package com.foodDelivery.restaurantService.mapper;

import com.foodDelivery.restaurantService.dto.MenuItemCreateRequest;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.model.MenuItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        menuItem.setAvailable(request.getAvailable() != null ? request.getAvailable() : true);
        menuItem.setItemType(request.getItemType());
        menuItem.setVegetarian(request.getVegetarian() != null ? request.getVegetarian() : false);
        menuItem.setNonVegetarian(request.getNonVegetarian() != null ? request.getNonVegetarian() : false);
        menuItem.setVegan(request.getVegan() != null ? request.getVegan() : false);
        menuItem.setGlutenFree(request.getGlutenFree() != null ? request.getGlutenFree() : false);
        menuItem.setUnit(request.getUnit());
        menuItem.setQuantity(request.getQuantity() != null ? request.getQuantity() : 0);
        menuItem.setExpiryDate(request.getExpiryDate());
        menuItem.setOnPromotion(request.getOnPromotion() != null ? request.getOnPromotion() : false);
        menuItem.setDiscountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0);
        menuItem.setDiscountedPrice(request.getDiscountedPrice() != null ? request.getDiscountedPrice() : 0);
        menuItem.setPreparationTimeMinutes(request.getPreparationTimeMinutes() != null ? request.getPreparationTimeMinutes() : 0);
        menuItem.setSpicy(request.getSpicy() != null ? request.getSpicy() : false);
        menuItem.setPopularityScore(0);

        // Map add-ons
        List<MenuItem.AddOn> addOns = new ArrayList<>();
        if (request.getAddOns() != null) {
            addOns = request.getAddOns().stream()
                    .map(addOnRequest -> {
                        MenuItem.AddOn addOn = new MenuItem.AddOn();
                        addOn.setId(UUID.randomUUID().toString());
                        addOn.setName(addOnRequest.getName());
                        addOn.setDescription(addOnRequest.getDescription());
                        addOn.setPrice(addOnRequest.getPrice());
                        addOn.setAvailable(addOnRequest.getAvailable() != null ? addOnRequest.getAvailable() : true);
                        addOn.setMultiple(addOnRequest.getMultiple() != null ? addOnRequest.getMultiple() : false);
                        addOn.setRequired(addOnRequest.getRequired() != null ? addOnRequest.getRequired() : false);
                        addOn.setMaxQuantity(addOnRequest.getMaxQuantity() != null ? addOnRequest.getMaxQuantity() : 1);
                        return addOn;
                    })
                    .collect(Collectors.toList());
        }
        menuItem.setAddOns(addOns);

        return menuItem;
    }

    public MenuItemResponse toResponse(MenuItem entity) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(entity.getId());
        response.setRestaurantId(entity.getRestaurantId());
        response.setCategoryId(entity.getCategoryId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setPrice(entity.getPrice());
        response.setImageUrl(entity.getImageUrl());
        response.setAvailable(entity.isAvailable());
        response.setItemType(entity.getItemType());
        response.setVegetarian(entity.isVegetarian());
        response.setNonVegetarian(entity.isNonVegetarian());
        response.setVegan(entity.isVegan());
        response.setGlutenFree(entity.isGlutenFree());
        response.setUnit(entity.getUnit());
        response.setQuantity(entity.getQuantity());
        response.setExpiryDate(entity.getExpiryDate());
        response.setOnPromotion(entity.isOnPromotion());
        response.setDiscountPercentage(entity.getDiscountPercentage());
        response.setDiscountedPrice(entity.getDiscountedPrice());
        response.setPreparationTimeMinutes(entity.getPreparationTimeMinutes());
        response.setSpicy(entity.isSpicy());
        response.setPopularityScore(entity.getPopularityScore());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Map add-ons
        List<MenuItemResponse.AddOnResponse> addOns = new ArrayList<>();
        if (entity.getAddOns() != null) {
            addOns = entity.getAddOns().stream()
                    .map(addOn -> {
                        MenuItemResponse.AddOnResponse addOnResponse = new MenuItemResponse.AddOnResponse();
                        addOnResponse.setId(addOn.getId());
                        addOnResponse.setName(addOn.getName());
                        addOnResponse.setDescription(addOn.getDescription());
                        addOnResponse.setPrice(addOn.getPrice());
                        addOnResponse.setAvailable(addOn.isAvailable());
                        addOnResponse.setMultiple(addOn.isMultiple());
                        addOnResponse.setRequired(addOn.isRequired());
                        addOnResponse.setMaxQuantity(addOn.getMaxQuantity());
                        return addOnResponse;
                    })
                    .collect(Collectors.toList());
        }
        response.setAddOns(addOns);

        return response;
    }
}