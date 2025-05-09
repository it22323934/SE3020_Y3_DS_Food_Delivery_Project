package com.foodDelivery.orderService.repository;

import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUserId(Long userId);
    List<Order> findByRestaurantId(String restaurantId);
    List<Order> findByRestaurantIdAndStatus(String restaurantId, OrderStatus status);
}