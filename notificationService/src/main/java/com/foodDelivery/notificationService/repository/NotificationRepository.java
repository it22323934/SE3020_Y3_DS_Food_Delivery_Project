// notificationService/src/main/java/com/foodDelivery/notificationService/repository/NotificationRepository.java
package com.foodDelivery.notificationService.repository;

import com.foodDelivery.notificationService.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserId(String userId);
    List<Notification> findByReferenceId(String referenceId);
}