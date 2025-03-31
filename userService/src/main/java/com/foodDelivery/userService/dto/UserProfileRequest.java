package com.foodDelivery.userService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phoneNumber;
}