package com.foodDelivery.orderService.service;

import com.foodDelivery.orderService.dto.OrderCreateRequest;
import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;

public interface KafkaProducerService {
    void sendOrderOutForDeliveryEvent(Order order, OrderCreateRequest orderDetails);
    void sendOrderStatusUpdateEvent(Order order, OrderStatus orderStatus);
    void sendOrderCreatedEvent(Order order, OrderCreateRequest orderDetails);
}