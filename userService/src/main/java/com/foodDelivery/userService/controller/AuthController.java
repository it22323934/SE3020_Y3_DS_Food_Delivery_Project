package com.foodDelivery.userService.controller;

import com.foodDelivery.userService.dto.LoginRequest;
import com.foodDelivery.userService.dto.MessageResponse;
import com.foodDelivery.userService.dto.PasswordResetRequest;
import com.foodDelivery.userService.dto.SignupRequest;
import com.foodDelivery.userService.serviceInterfaces.AuthService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private static final String AUTHENTICATION_SERVICE = "authenticationService";

    /**
     * User sign-in endpoint.
     * SECURITY FIX: Added rate limiting to prevent brute force attacks
     */
    @PostMapping("/signin")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "signInFallback")
    @RateLimiter(name = "authRateLimiter", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.authenticateUser(loginRequest);
    }

    public ResponseEntity<?> signInFallback(LoginRequest loginRequest, Exception e) {
        log.error("Authentication service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Authentication service is currently unavailable. Please try again later."));
    }

    public ResponseEntity<?> rateLimitFallback(LoginRequest loginRequest, Exception e) {
        log.warn("Rate limit exceeded for signin attempt");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new MessageResponse("Too many login attempts. Please try again later."));
    }

    @PostMapping("/signup")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "signUpFallback")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return authService.registerUser(signUpRequest);
    }

    public ResponseEntity<?> signUpFallback(SignupRequest signUpRequest, Exception e) {
        log.error("Registration service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Registration service is currently unavailable. Please try again later."));
    }

    @GetMapping("/confirm")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "confirmFallback")
    public ResponseEntity<?> confirmUserAccount(@RequestParam("token") String confirmationToken) {
        return authService.confirmUserAccount(confirmationToken);
    }

    public ResponseEntity<?> confirmFallback(String token, Exception e) {
        log.error("Confirmation service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Account confirmation service is currently unavailable. Please try again later."));
    }

    /**
     * Forgot password endpoint.
     * SECURITY FIX: Added rate limiting to prevent email bombing and enumeration attacks
     */
    @PostMapping("/forgot-password")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "forgotPasswordFallback")
    @RateLimiter(name = "authRateLimiter", fallbackMethod = "passwordResetRateLimitFallback")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        return authService.forgotPassword(email);
    }

    public ResponseEntity<?> forgotPasswordFallback(String email, Exception e) {
        log.error("Forgot Password reset service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Password reset service is currently unavailable. Please try again later."));
    }

    public ResponseEntity<?> passwordResetRateLimitFallback(String email, Exception e) {
        log.warn("Rate limit exceeded for password reset attempt");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new MessageResponse("Too many password reset attempts. Please try again later."));
    }

    @GetMapping("/password/validate")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "validateTokenFallback")
    public ResponseEntity<?> validateResetToken(@RequestParam("token") String token) {
        return authService.validateResetToken(token);
    }

    public ResponseEntity<?> validateTokenFallback(String token, Exception e) {
        log.error("Token validation service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Token validation service is currently unavailable. Please try again later."));
    }

    @PostMapping("/reset-password")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "resetPasswordFallback")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest) {
        return authService.resetPassword(resetRequest);
    }

    public ResponseEntity<?> resetPasswordFallback(PasswordResetRequest resetRequest, Exception e) {
        log.error("Password reset service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Password reset service is currently unavailable. Please try again later."));
    }
}