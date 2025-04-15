package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.event.RestaurantEvent;
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
}