// notificationService/src/main/java/com/foodDelivery/notificationService/repository/NotificationTemplateRepository.java
package com.foodDelivery.notificationService.repository;

import com.foodDelivery.notificationService.model.EventType;
import com.foodDelivery.notificationService.model.NotificationTemplate;
import com.foodDelivery.notificationService.model.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {
    Optional<NotificationTemplate> findByTypeAndEventTypeAndIsActiveTrue(NotificationType type, EventType eventType);
}