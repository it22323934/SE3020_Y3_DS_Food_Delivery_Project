package com.foodDelivery.userService.controller;

import com.foodDelivery.userService.config.JwtUtils;
import com.foodDelivery.userService.dto.*;
import com.foodDelivery.userService.event.UserRegistrationEvent;
import com.foodDelivery.userService.model.ConfirmationToken;
import com.foodDelivery.userService.model.PasswordResetToken;
import com.foodDelivery.userService.model.Role;
import com.foodDelivery.userService.model.User;
import com.foodDelivery.userService.repository.ConfirmationTokenRepository;
import com.foodDelivery.userService.repository.PasswordResetTokenRepository;
import com.foodDelivery.userService.repository.RoleRepository;
import com.foodDelivery.userService.repository.UserRepository;
import com.foodDelivery.userService.service.KafkaProducerService;
import com.foodDelivery.userService.service.UserDetailsImpl;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final KafkaProducerService kafkaProducerService;
    private static final String AUTHENTICATION_SERVICE = "authenticationService";
    private static final String RESET_PASSWORD_URL="http://localhost:5173/reset-password?token=";

    @PostMapping("/signin")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "signInFallback")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Use either username or email directly
            String loginIdentifier = loginRequest.getUsername();
            if (loginRequest.getEmail() != null && !loginRequest.getEmail().isEmpty()) {
                loginIdentifier = loginRequest.getEmail();
            }

            // First authenticate credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginIdentifier, loginRequest.getPassword()));

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Explicitly check enabled status from database
            Optional<User> userOptional = userRepository.findById(userDetails.getId());
            if (userOptional.isEmpty() || !userOptional.get().isEnabled()) {
                return ResponseEntity
                        .status(403)
                        .body(new MessageResponse("Error: Account is not verified. Please check your email to verify your account."));
            }

            // Continue with successful authentication
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getId(),
                    userDetails.getUsername(), userDetails.getEmail(), roles));
        } catch (Exception e) {
            return ResponseEntity
                    .status(401)
                    .body(new MessageResponse("Error: Invalid credentials. Please check your email/username and password."));
        }
    }

    public ResponseEntity<?> signInFallback(LoginRequest loginRequest, Exception e) {
        log.error("Authentication service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Authentication service is currently unavailable. Please try again later."));
    }

    @PostMapping("/signup")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "signUpFallback")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setPhoneNumber(signUpRequest.getPhoneNumber());

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "restaurant":
                        Role restaurantRole = roleRepository.findByName("ROLE_RESTAURANT_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(restaurantRole);
                        break;
                    case "delivery":
                        Role deliveryRole = roleRepository.findByName("ROLE_DELIVERY_PERSONNEL")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(deliveryRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName("ROLE_CUSTOMER")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Generate confirmation token
        ConfirmationToken confirmationToken = new ConfirmationToken(savedUser);
        confirmationTokenRepository.save(confirmationToken);

        String confirmationUrl = "http://localhost:8081/api/auth/confirm?token=" + confirmationToken.getToken();

        // Create and send registration event
        UserRegistrationEvent event = new UserRegistrationEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                "USER_REGISTERED",
                savedUser.getPhoneNumber() != null ? savedUser.getPhoneNumber() : "",
                confirmationUrl,
                System.currentTimeMillis()
        );

        kafkaProducerService.sendUserRegistrationEvent(event);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    public ResponseEntity<?> signUpFallback(SignupRequest signUpRequest, Exception e) {
        log.error("Registration service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Registration service is currently unavailable. Please try again later."));
    }

    @GetMapping("/confirm")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "confirmFallback")
    public ResponseEntity<?> confirmUserAccount(@RequestParam("token") String confirmationToken) {
        Optional<ConfirmationToken> token = confirmationTokenRepository.findByToken(confirmationToken);

        if (token.isPresent()) {
            User user = token.get().getUser();
            user.setEnabled(true);
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("Email confirmed successfully!"));
        } else {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid token!"));
        }
    }

    public ResponseEntity<?> confirmFallback(String token, Exception e) {
        log.error("Confirmation service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Account confirmation service is currently unavailable. Please try again later."));
    }

    @PostMapping("/forgot-password")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "555555555555555")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Generate password reset token
            String token = UUID.randomUUID().toString();
            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setToken(token);
            passwordResetToken.setUser(user);
            passwordResetToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            passwordResetTokenRepository.save(passwordResetToken);

            // Create reset URL
            String resetUrl = RESET_PASSWORD_URL + token;

            // Send password reset event through Kafka
            kafkaProducerService.sendPasswordResetEvent(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    "PASSWORD_RESET_REQUESTED",
                    resetUrl
            );
        }

        // Always return success for security reasons
        return ResponseEntity.ok(new MessageResponse(
                "If the email exists in our system, password reset instructions have been sent."));
    }

    public ResponseEntity<?> forgotPasswordFallback(String email, Exception e) {
        log.error("Password reset service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Password reset service is currently unavailable. Please try again later."));
    }

    @GetMapping("/password/validate")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "validateTokenFallback")
    public ResponseEntity<?> validateResetToken(@RequestParam("token") String token) {
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);

        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();

            if (resetToken.isExpired()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Token has expired!"));
            }

            return ResponseEntity.ok(new MessageResponse("Token is valid"));
        }

        return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: Invalid token!"));
    }

    public ResponseEntity<?> validateTokenFallback(String token, Exception e) {
        log.error("Token validation service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Token validation service is currently unavailable. Please try again later."));
    }

    @PostMapping("/reset-password")
    @CircuitBreaker(name = AUTHENTICATION_SERVICE, fallbackMethod = "resetPasswordFallback")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest resetRequest) {
        // Add proper logging
        log.info("Password reset requested with token: '{}'", resetRequest.getToken());
        String cleanToken = resetRequest.getToken().trim();
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(cleanToken);
        log.info("Token found in database: {}", tokenOptional.isPresent());
        if (tokenOptional.isPresent()) {
            PasswordResetToken resetToken = tokenOptional.get();

            if (resetToken.isExpired()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Token has expired!"));
            }

            User user = resetToken.getUser();
            user.setPassword(encoder.encode(resetRequest.getNewPassword()));
            userRepository.save(user);

            // Delete used token
            passwordResetTokenRepository.delete(resetToken);

            return ResponseEntity.ok(new MessageResponse("Password has been reset successfully!"));
        }

        return ResponseEntity.badRequest()
                .body(new MessageResponse("Error: Invalid token!"));
    }

    public ResponseEntity<?> resetPasswordFallback(PasswordResetRequest resetRequest, Exception e) {
        log.error("Password reset service is down or not responding: {}", e.getMessage());
        return ResponseEntity.status(503)
                .body(new MessageResponse("Password reset service is currently unavailable. Please try again later."));
    }
}