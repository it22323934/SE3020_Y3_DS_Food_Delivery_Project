// notificationService/src/main/java/com/foodDelivery/notificationService/event/OrderStatusEvent.java
package com.foodDelivery.notificationService.event;

import lombok.Data;

@Data
public class OrderStatusEvent {
    private Long orderId;
    private String status;
    private String customerEmail;
    private String customerPhone;
    private String customerId;
    private String restaurantId;
    private String driverId;
    private String customerName;
}