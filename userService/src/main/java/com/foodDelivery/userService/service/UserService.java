package com.foodDelivery.userService.service;

import com.foodDelivery.userService.dto.PasswordChangeRequest;
import com.foodDelivery.userService.dto.PasswordResetRequest;
import com.foodDelivery.userService.dto.UserProfileRequest;
import com.foodDelivery.userService.dto.UserProfileResponse;
import com.foodDelivery.userService.model.PasswordResetToken;
import com.foodDelivery.userService.model.User;
import com.foodDelivery.userService.repository.PasswordResetTokenRepository;
import com.foodDelivery.userService.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaProducerService kafkaProducerService;

    public Optional<UserProfileResponse> getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserProfileResponse);
    }

    public boolean updateUserProfile(String username, UserProfileRequest profileRequest) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    user.setFirstName(profileRequest.getFirstName());
                    user.setLastName(profileRequest.getLastName());
                    user.setPhoneNumber(profileRequest.getPhoneNumber());
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    public boolean changePassword(String username, PasswordChangeRequest request) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

    @CircuitBreaker(name = "passwordResetEmail", fallbackMethod = "passwordResetEmailFallback")
    public boolean requestPasswordReset(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setToken(UUID.randomUUID().toString());
                    token.setExpiryDate(LocalDateTime.now().plusHours(24));
                    passwordResetTokenRepository.save(token);

                    // Here you would send a notification through Kafka
                    // Similar to how you're doing it with registration

                    return true;
                })
                .orElse(false);
    }

    public boolean passwordResetEmailFallback(String email, Exception e) {
        log.error("Failed to process password reset for {}: {}", email, e.getMessage());
        return false;
    }

    public boolean resetPassword(PasswordResetRequest resetRequest) {
        return passwordResetTokenRepository.findByToken(resetRequest.getToken())
                .filter(token -> !token.isExpired())
                .map(token -> {
                    User user = token.getUser();
                    user.setPassword(passwordEncoder.encode(resetRequest.getNewPassword()));
                    userRepository.save(user);
                    passwordResetTokenRepository.delete(token);
                    return true;
                })
                .orElse(false);
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRoles().stream()
                        .map(role -> role.getName())
                        .toList()
        );
    }
}