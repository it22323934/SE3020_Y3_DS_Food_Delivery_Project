package com.foodDelivery.userService.event;

import com.foodDelivery.userService.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationAdminEvent {
    private Long userId;
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private String eventType;
    private String phoneNumber;
    private String confirmationUrl;
    private long timestamp;
}
