package com.foodDelivery.restaurantService.serviceInterfaces;

import com.foodDelivery.restaurantService.dto.PromotionCreateRequest;
import com.foodDelivery.restaurantService.dto.PromotionResponse;
import com.foodDelivery.restaurantService.dto.PromotionValidationRequest;

import java.util.List;

public interface PromotionService {
    PromotionResponse createPromotion(PromotionCreateRequest request, String token);
    PromotionResponse updatePromotion(String id, PromotionCreateRequest request, String token);
    void deletePromotion(String id, String token);
    PromotionResponse getPromotionById(String id);
    List<PromotionResponse> getPromotionsByRestaurantId(String restaurantId);
    PromotionResponse validatePromotion(PromotionValidationRequest request);
    List<PromotionResponse> getActivePromotionsByRestaurantId(String restaurantId);
}