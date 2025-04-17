package com.foodDelivery.notificationService.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceAuthRequestInterceptor implements RequestInterceptor {

    private final TokenManager tokenManager;

    @Override
    public void apply(RequestTemplate template) {
        String url = template.url();
        log.debug("Intercepting request to: {}", url);

        // Add token to ALL requests (including cuisine-types)
        String token = tokenManager.getCurrentToken();
        if (token != null && !token.isEmpty()) {
            log.debug("Adding auth token to request: {}", url);
            template.header(HttpHeaders.AUTHORIZATION, token);
        } else {
            log.warn("Token is null or empty, authentication may fail for: {}", url);
        }
    }
}