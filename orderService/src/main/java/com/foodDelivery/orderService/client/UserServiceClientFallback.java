package com.foodDelivery.orderService.client;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceClientFallback {

    public boolean validateUserRole(String userId, String role, String token) {
        log.error("Fallback: Error validating user role for user {}", userId);
        return false;
    }

    public boolean validateUserRoleById(String userId, String role, String token) {
        log.error("Fallback: Error validating user role by ID for user {}", userId);
        return false;
    }

    public Long getUserIdFromToken(String token) {
        log.error("Fallback: Failed to retrieve user ID from token");
        return null;
    }
}
