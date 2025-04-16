package com.foodDelivery.notificationService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Bean
    public ServiceAuthRequestInterceptor serviceAuthRequestInterceptor(TokenManager tokenManager) {
        return new ServiceAuthRequestInterceptor(tokenManager);
    }
}