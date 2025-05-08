package com.foodDelivery.orderService.service;

import com.foodDelivery.orderService.dto.OrderCreateRequest;
import com.foodDelivery.orderService.model.Order;

public interface KafkaProducerService {
    void sendOrderOutForDeliveryEvent(Order order, OrderCreateRequest orderDetails);
    void sendOrderStatusUpdateEvent(Order order);
}