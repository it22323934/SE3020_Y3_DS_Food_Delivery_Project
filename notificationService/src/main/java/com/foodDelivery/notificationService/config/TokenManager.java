package com.foodDelivery.notificationService.config;

import com.foodDelivery.notificationService.client.AuthServiceClient;
import com.foodDelivery.notificationService.dto.AuthResponse;
import com.foodDelivery.notificationService.dto.LoginRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenManager {

    private final AuthServiceClient authServiceClient;

    @Value("${service.auth.username}")
    private String serviceUsername;

    @Value("${service.auth.password}")
    private String servicePassword;

    private String currentToken;

    @PostConstruct
    public void init() {
        refreshToken();
        log.info("Token manager initialized");
    }

    @Scheduled(fixedRate = 3600000) // Refresh token every hour
    public void refreshToken() {
        try {
            log.info("Refreshing service authentication token");
            LoginRequest loginRequest = new LoginRequest(serviceUsername, servicePassword);
            AuthResponse response = authServiceClient.login(loginRequest).getBody();

            if (response != null && response.getToken() != null) {
                currentToken = "Bearer " + response.getToken();
                log.info("Token refreshed successfully {}", currentToken);
            } else {
                log.error("Failed to refresh token - null response");
            }
        } catch (Exception e) {
            log.error("Error refreshing token", e);
        }
    }

    public String getCurrentToken() {
        if (currentToken == null) {
            refreshToken();
        }
        return currentToken;
    }
}