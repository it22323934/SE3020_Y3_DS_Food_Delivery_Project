package com.foodDelivery.notificationService.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String type;
    private Long expiresIn;
}