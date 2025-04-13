package com.foodDelivery.userService.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePictureUrl;
    private String address;
    private String locationType;
    private Double latitude;
    private Double longitude;
    private java.util.List<String> roles;
}