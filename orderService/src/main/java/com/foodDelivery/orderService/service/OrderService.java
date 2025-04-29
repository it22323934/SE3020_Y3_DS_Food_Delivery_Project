package com.foodDelivery.orderService.service;

import com.foodDelivery.orderService.dto.OrderCreateRequest;
import com.foodDelivery.orderService.dto.OrderResponse;
import com.foodDelivery.orderService.model.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderCreateRequest request, String token);
    OrderResponse updateOrderStatus(String orderId, OrderStatus status, String token);
    OrderResponse getOrderById(String orderId, String token);
    List<OrderResponse> getOrdersByUserId(Long userId, String token);
    List<OrderResponse> getOrdersByRestaurantId(String restaurantId, String token);
    void cancelOrder(String orderId, String token);
}