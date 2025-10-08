package com.foodDelivery.orderService.service.impl;

import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;
import com.foodDelivery.orderService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("orderAuthorizationService")
@RequiredArgsConstructor
@Slf4j
public class OrderAuthorizationService {

    private final OrderRepository orderRepository;

    public boolean canAccessOrder(String orderId, Authentication authentication) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;

        // Admin can access all orders
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // User can access their own orders
        Long userId = (Long) ((Map<String, Object>) authentication.getDetails()).get("userId");
        return order.getUserId().equals(userId);
    }

    /**
     * Check if user can access orders for a specific userId
     * @param userId The user ID whose orders are being accessed
     * @param authentication Current authenticated user
     * @return true if authorized, false otherwise
     */
    public boolean canAccessUserOrders(Long userId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Unauthorized access attempt to orders for user: {}", userId);
            return false;
        }

        // Admin can access all user orders
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Restaurant admin can access orders for their restaurants (handled separately)
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_ADMIN"))) {
            return true;
        }

        // Users can only access their own orders
        // Extract userId from authentication details
        try {
            Object details = authentication.getDetails();
            if (details instanceof Map) {
                @SuppressWarnings("unchecked")
                Long authenticatedUserId = ((Map<String, Long>) details).get("userId");
                if (authenticatedUserId != null && authenticatedUserId.equals(userId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error extracting user ID from authentication: {}", e.getMessage());
        }

        log.warn("User {} attempted to access orders for user {}",
                authentication.getName(), userId);
        return false;
    }

    public boolean canCancelOrder(String orderId, Authentication authentication) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return false;

        // Only allow cancellation for PENDING or CONFIRMED status
        if (order.getStatus() != OrderStatus.PENDING &&
            order.getStatus() != OrderStatus.CONFIRMED) {
            return false;
        }

        return canAccessOrder(orderId, authentication);
    }
}