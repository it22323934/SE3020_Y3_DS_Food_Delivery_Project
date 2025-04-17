package com.foodDelivery.notificationService.client;

import com.foodDelivery.notificationService.dto.AuthResponse;
import com.foodDelivery.notificationService.dto.LoginRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "authService", url = "${user.service.url}")
public interface AuthServiceClient {
    @PostMapping("/api/auth/signin")
    ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request);
}