package com.foodDelivery.orderService.service.impl;

import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;
import com.foodDelivery.orderService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
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