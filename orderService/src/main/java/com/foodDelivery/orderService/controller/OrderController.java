package com.foodDelivery.orderService.controller;

import com.foodDelivery.orderService.dto.OrderCreateRequest;
import com.foodDelivery.orderService.dto.OrderResponse;
import com.foodDelivery.orderService.exception.BusinessValidationException;
import com.foodDelivery.orderService.model.OrderStatus;
import com.foodDelivery.orderService.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private static final String ORDER_SERVICE = "orderService";

    @PostMapping
    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "createOrderFallback")
    @Retry(name = ORDER_SERVICE)
    public ResponseEntity<?> createOrder(
            @RequestBody OrderCreateRequest request,
            @RequestHeader("Authorization") String token) {
        log.info("Creating new order for user: {}", request.getUserId());
        OrderResponse response = orderService.createOrder(request, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{orderId}/status")
    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "updateOrderStatusFallback")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'ADMIN')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus status,
            @RequestHeader("Authorization") String token) {
        log.info("Updating order status to {} for order: {}", status, orderId);
        OrderResponse response = orderService.updateOrderStatus(orderId, status, token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "getOrderByIdFallback")
    @PreAuthorize("@orderAuthorizationService.canAccessOrder(#orderId, authentication)")
    public ResponseEntity<?> getOrderById(
            @PathVariable String orderId,
            @RequestHeader("Authorization") String token) {
        log.info("Fetching order details for ID: {}", orderId);
        OrderResponse response = orderService.getOrderById(orderId, token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "getOrdersByUserIdFallback")
    @PreAuthorize("@orderAuthorizationService.canAccessUserOrders(#userId, authentication)")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(
            @PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        log.info("Fetching orders for user: {}", userId);
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId, token);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "getOrdersByRestaurantIdFallback")
    @PreAuthorize("hasAnyRole('RESTAURANT_ADMIN', 'ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersByRestaurantId(
            @PathVariable String restaurantId,
            @RequestHeader("Authorization") String token) {
        log.info("Fetching orders for restaurant: {}", restaurantId);
        List<OrderResponse> orders = orderService.getOrdersByRestaurantId(restaurantId, token);
        return ResponseEntity.ok(orders);
    }

    @DeleteMapping("/{orderId}")
    @CircuitBreaker(name = ORDER_SERVICE, fallbackMethod = "cancelOrderFallback")
    @PreAuthorize("@orderAuthorizationService.canCancelOrder(#orderId, authentication)")
    public ResponseEntity<?> cancelOrder(
            @PathVariable String orderId,
            @RequestHeader("Authorization") String token) {
        log.info("Cancelling order: {}", orderId);
        orderService.cancelOrder(orderId, token);
        return ResponseEntity.noContent().build();
    }

    // Fallback methods with proper error responses
    private ResponseEntity<?> createOrderFallback(
            OrderCreateRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to create order", e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Unable to create order at this time"));
    }

    private ResponseEntity<?> updateOrderStatusFallback(
            String orderId, OrderStatus status, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to update order status for order: {}", orderId, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Unable to update order status at this time"));
    }

    private ResponseEntity<?> getOrderByIdFallback(
            String orderId, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch order: {}", orderId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Unable to fetch order at this time"));
    }

    private ResponseEntity<List<OrderResponse>> getOrdersByUserIdFallback(
            Long userId, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch orders for user: {}", userId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    private ResponseEntity<List<OrderResponse>> getOrdersByRestaurantIdFallback(
            String restaurantId, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to fetch orders for restaurant: {}", restaurantId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    private ResponseEntity<?> cancelOrderFallback(
            String orderId, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to cancel order: {}", orderId, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Unable to cancel order at this time"));
    }
}