package com.foodDelivery.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profilePicture;
    private String address;
    private String identificationNumber;
    private String vehicleNumber;
    private Boolean verified;
    private Boolean disabled;
    private LocationDTO location;
    @Data
    public static class LocationDTO {
        private String type;
        private double[] coordinates;
        private String address;
    }
    private Set<String> roles;
}