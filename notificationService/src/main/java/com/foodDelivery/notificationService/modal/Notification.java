// notificationService/src/main/java/com/foodDelivery/notificationService/model/Notification.java
package com.foodDelivery.notificationService.modal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(collection = "notifications")
public class Notification {
    @Id
    private String notificationId;
    private String userId;
    private String templateId;
    private String content;
    private NotificationType type;
    private NotificationStatus status;
    private String referenceId;
    private String referenceType;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
}