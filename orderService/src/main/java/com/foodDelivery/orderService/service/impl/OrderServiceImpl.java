package com.foodDelivery.orderService.service.impl;

import com.foodDelivery.orderService.client.RestaurantServiceClient;
import com.foodDelivery.orderService.dto.OrderCreateRequest;
import com.foodDelivery.orderService.dto.OrderResponse;
import com.foodDelivery.orderService.exception.BusinessValidationException;
import com.foodDelivery.orderService.exception.OrderNotFoundException;
import com.foodDelivery.orderService.exception.RestaurantNotFoundException;
import com.foodDelivery.orderService.mapper.OrderMapper;
import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;
import com.foodDelivery.orderService.repository.OrderRepository;
import com.foodDelivery.orderService.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantServiceClient restaurantServiceClient;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String token) {
        log.info("Creating order for restaurant: {}", request.getRestaurantId());
        
        // Verify restaurant exists and is active
        com.foodDelivery.orderService.dto.restaurant.RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(
                request.getRestaurantId(), token);
        
        if (restaurant == null) {
            throw new RestaurantNotFoundException("Restaurant not found: " + request.getRestaurantId());
        }
        
        if (!restaurant.isEnabled()) {
            throw new IllegalStateException("Restaurant is not active: " + request.getRestaurantId());
        }

        Order order = orderMapper.toEntity(request);
        order = orderRepository.save(order);
        
        // TODO: Publish order created event to Kafka
        
        log.info("Order created successfully with ID: {}", order.getId());
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus status, String token) {
        log.info("Updating order status: {} for order: {}", status, orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // Validate status transition
        validateStatusTransition(order.getStatus(), status);

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        
        // TODO: Publish order status updated event to Kafka
        
        log.info("Order status updated successfully for order: {}", orderId);
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponse getOrderById(String orderId, String token) {
        log.info("Fetching order details for ID: {}", orderId);
        
        return orderRepository.findById(orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId, String token) {
        log.info("Fetching orders for user: {}", userId);
        
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByRestaurantId(String restaurantId, String token) {
        log.info("Fetching orders for restaurant: {}", restaurantId);
        
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId, String token) {
        log.info("Cancelling order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (!canCancel(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        
        // TODO: Publish order cancelled event to Kafka
        
        log.info("Order cancelled successfully: {}", orderId);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Add validation logic for status transitions
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update cancelled order");
        }
        
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot update delivered order");
        }
        
        // Add more specific transition rules as needed
    }

    private boolean canCancel(OrderStatus status) {
        return status == OrderStatus.PENDING || 
               status == OrderStatus.CONFIRMED;
    }

    private void validateOrderRequest(OrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessValidationException("Order must contain at least one item");
        }

        if (request.getDeliveryLocation() == null) {
            throw new BusinessValidationException("Delivery location is required");
        }

        if (request.getContactInfo() == null) {
            throw new BusinessValidationException("Contact information is required");
        }

        // Validate total calculation
        double calculatedTotal = request.getSubtotal() + request.getTaxAmount() +
                request.getDeliveryFee() - request.getDiscount();
        if (Math.abs(calculatedTotal - request.getTotal()) > 0.01) {
            throw new BusinessValidationException("Invalid order total calculation");
        }
    }
}