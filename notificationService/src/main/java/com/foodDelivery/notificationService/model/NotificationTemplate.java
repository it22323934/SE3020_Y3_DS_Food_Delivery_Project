// notificationService/src/main/java/com/foodDelivery/notificationService/model/NotificationTemplate.java
package com.foodDelivery.notificationService.model;

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

    public enum NotificationType {
        EMAIL, SMS, PUSH, IN_APP
    }

    public enum EventType {
        USER_REGISTRATION, ORDER_PLACED, ORDER_CONFIRMED, ORDER_READY,
        ORDER_PICKED_UP, ORDER_DELIVERED, PAYMENT_RECEIVED
    }
}