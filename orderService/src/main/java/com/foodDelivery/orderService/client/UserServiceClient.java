package com.foodDelivery.orderService.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Feign client for communicating with the User Service.
 * Used for authorization checks and retrieving user information.
 */
@FeignClient(
        name = "userService",
        url = "${user.service.url}"
)
public interface UserServiceClient {

    /**
     * Get user ID from authentication token.
     * Used for authorization checks to verify user ownership.
     *
     * @param token Authorization token (Bearer token)
     * @return User ID
     */
    @GetMapping("/api/users/getUserId")
    @CircuitBreaker(name = "userService")
    Long getUserIdFromToken(@RequestHeader("Authorization") String token);
}
