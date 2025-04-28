package com.foodDelivery.restaurantService.serviceInterfaces;

import com.foodDelivery.restaurantService.model.Promotion;

import java.util.List;

public interface KafkaProducerService {
    void sendRestaurantCreatedEvent(String restaurantId, String restaurantName,
                                    String email, String phoneNumber,
                                    List<String> adminIds, List<String> cuisineTypeIds);

    void sendRestaurantUpdatedEvent(String restaurantId, String restaurantName,
                                    String email, String phoneNumber,
                                    List<String> adminIds,
                                    List<String> addedAdminIds, List<String> removedAdminIds,
                                    List<String> cuisineTypeIds);

    void publishPromotionCreatedEvent(Promotion promotion);

    void publishPromotionUpdatedEvent(Promotion promotion);

    void publishPromotionDeletedEvent(Promotion promotion);
}