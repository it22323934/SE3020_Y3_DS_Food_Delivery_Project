package com.foodDelivery.restaurantService.mapper;

import com.foodDelivery.restaurantService.dto.PromotionCreateRequest;
import com.foodDelivery.restaurantService.dto.PromotionResponse;
import com.foodDelivery.restaurantService.model.Promotion;

import java.time.LocalDateTime;
import java.util.HashSet;

public class PromotionMapper {
    public static Promotion toEntity(PromotionCreateRequest request) {
        Promotion promotion = new Promotion();
        promotion.setRestaurantId(request.getRestaurantId());
        promotion.setCode(request.getCode().toUpperCase());
        promotion.setDescription(request.getDescription());
        promotion.setDiscountPercentage(request.getDiscountPercentage());
        promotion.setMaxDiscount(request.getMaxDiscount());
        promotion.setMinOrderAmount(request.getMinOrderAmount());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setMaxUses(request.getMaxUses());
        promotion.setOneTimeUsePerUser(request.isOneTimeUsePerUser());
        promotion.setActive(true);
        promotion.setCurrentUses(0);
        promotion.setUsedByUsers(new HashSet<>());
        promotion.setCreatedAt(LocalDateTime.now());
        promotion.setUpdatedAt(LocalDateTime.now());
        return promotion;
    }

    public static PromotionResponse toResponse(Promotion promotion) {
        PromotionResponse response = new PromotionResponse();
        response.setId(promotion.getId());
        response.setRestaurantId(promotion.getRestaurantId());
        response.setCode(promotion.getCode());
        response.setDescription(promotion.getDescription());
        response.setDiscountPercentage(promotion.getDiscountPercentage());
        response.setMaxDiscount(promotion.getMaxDiscount());
        response.setMinOrderAmount(promotion.getMinOrderAmount());
        response.setStartDate(promotion.getStartDate());
        response.setEndDate(promotion.getEndDate());
        response.setActive(promotion.isActive());
        response.setMaxUses(promotion.getMaxUses());
        response.setCurrentUses(promotion.getCurrentUses());
        response.setOneTimeUsePerUser(promotion.isOneTimeUsePerUser());
        response.setCreatedAt(promotion.getCreatedAt());
        response.setUpdatedAt(promotion.getUpdatedAt());
        return response;
    }
}