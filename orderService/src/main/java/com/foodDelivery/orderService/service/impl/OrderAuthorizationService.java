package com.foodDelivery.orderService.service.impl;

import com.foodDelivery.orderService.client.UserServiceClient;
import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;
import com.foodDelivery.orderService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for order authorization checks.
 * SECURITY FIX: Replaced fragile Map extraction from authentication.getDetails()
 * with proper UserServiceClient call to get user ID from token.
 */
@Service("orderAuthorizationService")
@RequiredArgsConstructor
@Slf4j
public class OrderAuthorizationService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;

    /**
     * Check if authenticated user can access a specific order.
     * SECURITY FIX: Now properly retrieves user ID from token via UserServiceClient
     */
    public boolean canAccessOrder(String orderId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Unauthorized access attempt to order: {}", orderId);
            return false;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            return false;
        }

        // Admin can access all orders
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            log.debug("Admin user {} accessing order {}", authentication.getName(), orderId);
            return true;
        }

        // User can access their own orders
        try {
            String token = getAuthorizationToken();
            if (token == null) {
                log.error("No authorization token found in request");
                return false;
            }

            Long userId = userServiceClient.getUserIdFromToken(token);
            if (userId == null) {
                log.error("Failed to retrieve user ID from token");
                return false;
            }

            boolean hasAccess = order.getUserId().equals(userId);
            if (!hasAccess) {
                log.warn("User {} (ID: {}) attempted to access order {} belonging to user {}",
                        authentication.getName(), userId, orderId, order.getUserId());
            }
            return hasAccess;
        } catch (Exception e) {
            log.error("Error checking order access for user {}: {}", authentication.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Check if user can access orders for a specific userId.
     * SECURITY FIX: Now properly retrieves user ID from token via UserServiceClient
     *
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
            log.debug("Admin user {} accessing orders for user {}", authentication.getName(), userId);
            return true;
        }

        // Restaurant admin - note this is permissive, actual restaurant-specific checks
        // should be done at service layer for their own restaurants
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_RESTAURANT_ADMIN"))) {
            log.debug("Restaurant admin {} accessing orders", authentication.getName());
            return true;
        }

        // Users can only access their own orders
        try {
            String token = getAuthorizationToken();
            if (token == null) {
                log.error("No authorization token found in request");
                return false;
            }

            Long authenticatedUserId = userServiceClient.getUserIdFromToken(token);
            if (authenticatedUserId == null) {
                log.error("Failed to retrieve user ID from token");
                return false;
            }

            boolean hasAccess = authenticatedUserId.equals(userId);
            if (!hasAccess) {
                log.warn("User {} (ID: {}) attempted to access orders for user {}",
                        authentication.getName(), authenticatedUserId, userId);
            }
            return hasAccess;
        } catch (Exception e) {
            log.error("Error checking user orders access for {}: {}", authentication.getName(), e.getMessage());
            return false;
        }
    }

    /**
     * Check if user can cancel a specific order.
     * Users can only cancel orders that are in PENDING or CONFIRMED status.
     */
    public boolean canCancelOrder(String orderId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Unauthorized order cancellation attempt for order: {}", orderId);
            return false;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order not found for cancellation: {}", orderId);
            return false;
        }

        // Only allow cancellation for PENDING or CONFIRMED status
        if (order.getStatus() != OrderStatus.PENDING &&
            order.getStatus() != OrderStatus.CONFIRMED) {
            log.warn("Cannot cancel order {} in status {}", orderId, order.getStatus());
            return false;
        }

        return canAccessOrder(orderId, authentication);
    }

    /**
     * Helper method to extract Authorization token from current HTTP request.
     *
     * @return Authorization token (without "Bearer " prefix), or null if not found
     */
    private String getAuthorizationToken() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader;
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving authorization token from request: {}", e.getMessage());
        }
        return null;
    }
}