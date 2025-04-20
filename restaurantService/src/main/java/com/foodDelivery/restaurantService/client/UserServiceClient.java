package com.foodDelivery.restaurantService.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
    private final WebClient.Builder webClientBuilder;

    @Value("${userService.baseUrl}")
    private String userServiceBaseUrl;

    public boolean validateUserRole(String userId, String role, String token) {
        try {
            Boolean result = webClientBuilder.build()
                    .get()
                    .uri(userServiceBaseUrl + "/api/users/validate?userName={userId}&role={role}",
                            userId, role)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .onErrorResume(e -> {
                        log.error("Error validating user role: {}", e.getMessage());
                        return Mono.just(false);
                    })
                    .block();

            return result != null && result;
        } catch (Exception e) {
            log.error("Failed to connect to user service", e);
            return false;
        }
    }

    public boolean validateUserRoleById(String userId, String role, String token) {
        try {
            Boolean result = webClientBuilder.build()
                    .get()
                    .uri(userServiceBaseUrl + "/api/users/validate?userId={userId}&role={role}",
                            userId, role)
                    .header(HttpHeaders.AUTHORIZATION, token)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .onErrorResume(e -> {
                        log.error("Error validating user role: {}", e.getMessage());
                        return Mono.just(false);
                    })
                    .block();

            return result != null && result;
        } catch (Exception e) {
            log.error("Failed to connect to user service", e);
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            log.error("Token is null or empty");
            return null;
        }

        // Make sure we handle Bearer prefix correctly
        String tokenValue = token;
        if (token.startsWith("Bearer ")) {
            tokenValue = token.substring(7);
        }

        try {
            log.info("Fetching user ID from token");

            Long userId = webClientBuilder.build()
                    .get()
                    .uri(userServiceBaseUrl + "/api/users/getUserId")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValue)
                    .retrieve()
                    .bodyToMono(Long.class)
                    .block();  // This is blocking and should be used carefully

            log.info("User ID retrieved: {}", userId);
            return userId;
        } catch (Exception e) {
            log.error("Failed to retrieve user ID from user service: {}", e.getMessage(), e);
            return null;
        }
    }
}