package com.foodDelivery.userService.dto;

import lombok.Data;

@Data
public class UserProfileRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String username;
    private String email;
    private String address;
    private String profilePicture;
    private LocationDTO location;

    @Data
    public static class LocationDTO {
        private String type;
        private double[] coordinates;
        private String address;
    }
}