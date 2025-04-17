// notificationService/src/main/java/com/foodDelivery/notificationService/model/NotificationTemplate.java
package com.foodDelivery.notificationService.modal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "notificationTemplates")
public class NotificationTemplate {
    @Id
    private String templateId;
    private NotificationType type;
    private EventType eventType;
    private String subject;
    private String content;
    private String variables;
    private boolean isActive;
}