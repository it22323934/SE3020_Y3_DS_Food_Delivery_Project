package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.event.PromotionCreatedEvent;
import com.foodDelivery.restaurantService.event.RestaurantEvent;
import com.foodDelivery.restaurantService.model.Promotion;
import com.foodDelivery.restaurantService.serviceInterfaces.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String NOTIFICATION_TOPIC = "restaurant-notifications";
    private static final String PROMOTION_TOPIC = "promotion-notifications";

    @Override
    public void sendRestaurantCreatedEvent(String restaurantId, String restaurantName,
                                           String email, String phoneNumber,
                                           List<String> adminIds, List<String> cuisineTypeIds) {
        try {
            RestaurantEvent event = RestaurantEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("RESTAURANT_CREATED")
                    .restaurantId(restaurantId)
                    .restaurantName(restaurantName)
                    .restaurantEmail(email)
                    .restaurantPhone(phoneNumber)
                    .adminIds(adminIds)
                    .cuisineTypeIds(cuisineTypeIds)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, restaurantId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Restaurant creation notification sent: {}", restaurantName);
                        } else {
                            log.error("Failed to send restaurant creation notification: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending restaurant creation notification: {}", e.getMessage());
        }
    }

    @Override
    public void sendRestaurantUpdatedEvent(String restaurantId, String restaurantName,
                                           String email, String phoneNumber,
                                           List<String> adminIds,
                                           List<String> addedAdminIds, List<String> removedAdminIds,
                                           List<String> cuisineTypeIds) {
        try {
            RestaurantEvent event = RestaurantEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("RESTAURANT_UPDATED")
                    .restaurantId(restaurantId)
                    .restaurantName(restaurantName)
                    .restaurantEmail(email)
                    .restaurantPhone(phoneNumber)
                    .adminIds(adminIds)
                    .addedAdminIds(addedAdminIds)
                    .removedAdminIds(removedAdminIds)
                    .cuisineTypeIds(cuisineTypeIds)
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, restaurantId, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Restaurant update notification sent: {}", restaurantName);
                        } else {
                            log.error("Failed to send restaurant update notification: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending restaurant update notification: {}", e.getMessage());
        }
    }

    public void publishPromotionCreatedEvent(Promotion promotion) {
        try {
            PromotionCreatedEvent event = PromotionCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PROMOTION_CREATED")
                    .restaurantId(promotion.getRestaurantId())
                    .restaurantName(promotion.getRestaurantName())
                    .code(promotion.getCode())
                    .description(promotion.getDescription())
                    .discountPercentage(promotion.getDiscountPercentage())
                    .minOrderAmount(promotion.getMinOrderAmount())
                    .maxDiscount(promotion.getMaxDiscount())
                    .startDate(promotion.getStartDate())
                    .endDate(promotion.getEndDate())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, promotion.getRestaurantId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Promotion creation notification sent for restaurant: {}",
                                    promotion.getRestaurantName());
                        } else {
                            log.error("Failed to send promotion creation notification: {}",
                                    ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending promotion creation notification: {}", e.getMessage());
        }
    }

    @Override
    public void publishPromotionUpdatedEvent(Promotion promotion) {
        try {
            PromotionCreatedEvent event = PromotionCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PROMOTION_UPDATED")
                    .restaurantId(promotion.getRestaurantId())
                    .restaurantName(promotion.getRestaurantName())
                    .code(promotion.getCode())
                    .description(promotion.getDescription())
                    .discountPercentage(promotion.getDiscountPercentage())
                    .minOrderAmount(promotion.getMinOrderAmount())
                    .maxDiscount(promotion.getMaxDiscount())
                    .startDate(promotion.getStartDate())
                    .endDate(promotion.getEndDate())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, promotion.getRestaurantId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Promotion update notification sent for restaurant: {}",
                                    promotion.getRestaurantName());
                        } else {
                            log.error("Failed to send promotion update notification: {}",
                                    ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending promotion update notification: {}", e.getMessage());
        }
    }

    @Override
    public void publishPromotionDeletedEvent(Promotion promotion) {
        try {
            PromotionCreatedEvent event = PromotionCreatedEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("PROMOTION_DELETED")
                    .restaurantId(promotion.getRestaurantId())
                    .restaurantName(promotion.getRestaurantName())
                    .code(promotion.getCode())
                    .description(promotion.getDescription())
                    .discountPercentage(promotion.getDiscountPercentage())
                    .minOrderAmount(promotion.getMinOrderAmount())
                    .maxDiscount(promotion.getMaxDiscount())
                    .startDate(promotion.getStartDate())
                    .endDate(promotion.getEndDate())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(NOTIFICATION_TOPIC, promotion.getRestaurantId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Promotion deletion notification sent for restaurant: {}",
                                    promotion.getRestaurantName());
                        } else {
                            log.error("Failed to send promotion deletion notification: {}",
                                    ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending promotion deletion notification: {}", e.getMessage());
        }
    }
}