// notificationService/src/main/java/com/foodDelivery/notificationService/model/EventType.java
package com.foodDelivery.notificationService.model;

public enum EventType {
    USER_REGISTRATION,
    ORDER_PLACED,
    ORDER_CONFIRMED,
    ORDER_READY,
    ORDER_PICKED_UP,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCESSFUL,
    PAYMENT_FAILED,
    DRIVER_ASSIGNED,
    PASSWORD_RESET_REQUESTED
}