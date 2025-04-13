package com.foodDelivery.userService.controller;

import com.foodDelivery.userService.config.JwtUtils;
import com.foodDelivery.userService.dto.*;
import com.foodDelivery.userService.modal.User;
import com.foodDelivery.userService.repository.RoleRepository;
import com.foodDelivery.userService.repository.UserRepository;
import com.foodDelivery.userService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static org.apache.kafka.common.requests.FetchMetadata.log;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private static final String AUTHENTICATION_SERVICE = "authenticationService";
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userService.getUserProfile(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfileRequest profileRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        try {
            boolean updated = userService.updateUserProfile(username, profileRequest);
            return updated
                    ? ResponseEntity.ok(new MessageResponse("Profile updated successfully"))
                    : ResponseEntity.badRequest().body(new MessageResponse("Failed to update profile"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
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

    @PostMapping("/signout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logoutUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Log the logout attempt
        log.info("User logout requested");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            if (jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                log.info("User {} successfully logged out", username);
            }
        }

        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateUserRole(
            @RequestParam String userId,
            @RequestParam String role) {
        // Use proper logging with @Slf4j annotation at the class level
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserController.class);
        log.info("Validating role {} for user {}", role, userId);

        try {
            // Get user from repository - adjust ID type if needed
            Optional<User> userOpt = userRepository.findById(Long.valueOf(userId));
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", userId);
                return ResponseEntity.ok(false);
            }

            User user = userOpt.get();
            boolean hasRole = user.getRoles().stream()
                    .anyMatch(userRole -> {
                        return userRole.getName().equals(role);
                    });

            log.info("User {} has role {}: {}", userId, role, hasRole);
            return ResponseEntity.ok(hasRole);
        } catch (Exception e) {
            log.error("Error validating role: {}", e.getMessage());
            return ResponseEntity.ok(false);
        }
    }

    @GetMapping("/all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "adminCreateUserFallback")
    public ResponseEntity<?> adminCreateUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            // Validate required fields
            if (signUpRequest.getUsername() == null || signUpRequest.getEmail() == null || signUpRequest.getPassword() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Username, email and password are required!"));
            }

            UserProfileResponse createdUser = userService.createUserByAdmin(signUpRequest);
            return ResponseEntity.ok(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/update-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminUpdateUser(@PathVariable Long userId, @Valid @RequestBody UpdateProfileRequest signUpRequest) {
        try {
            UserProfileResponse updatedUser = userService.updateUserByAdmin(userId, signUpRequest);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Failed to update user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> adminCreateUserFallback(SignupRequest signUpRequest, Exception e) {
        log.error("Admin user creation service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new MessageResponse("User creation service is currently unavailable. Please try again later."));
    }

}