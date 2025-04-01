package com.foodDelivery.userService.controller;

import com.foodDelivery.userService.dto.MessageResponse;
import com.foodDelivery.userService.dto.PasswordChangeRequest;
import com.foodDelivery.userService.dto.UserProfileRequest;
import com.foodDelivery.userService.dto.UserProfileResponse;
import com.foodDelivery.userService.repository.UserRepository;
import com.foodDelivery.userService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userService.getUserProfile(username)
                .map(profile -> ResponseEntity.ok(profile))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileRequest profileRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userService.updateUserProfile(username, profileRequest)
                ? ResponseEntity.ok(new MessageResponse("Profile updated successfully"))
                : ResponseEntity.badRequest().body(new MessageResponse("Failed to update profile"));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest passwordRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userService.changePassword(username, passwordRequest)
                ? ResponseEntity.ok(new MessageResponse("Password changed successfully"))
                : ResponseEntity.badRequest().body(new MessageResponse("Current password is incorrect"));
    }
}