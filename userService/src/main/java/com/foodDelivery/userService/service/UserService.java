package com.foodDelivery.userService.service;

import com.foodDelivery.userService.dto.*;
import com.foodDelivery.userService.event.UserRegistrationAdminEvent;
import com.foodDelivery.userService.model.ConfirmationToken;
import com.foodDelivery.userService.model.PasswordResetToken;
import com.foodDelivery.userService.model.Role;
import com.foodDelivery.userService.model.User;
import com.foodDelivery.userService.repository.ConfirmationTokenRepository;
import com.foodDelivery.userService.repository.PasswordResetTokenRepository;
import com.foodDelivery.userService.repository.RoleRepository;
import com.foodDelivery.userService.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final KafkaProducerService kafkaProducerService;
    private static final String CONFIRMATION_URL = "http://localhost:8081/api/auth/confirm?token=";

    public Optional<UserProfileResponse> getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .map(this::mapToUserProfileResponse);
    }

    public boolean updateUserProfile(String username, UserProfileRequest profileRequest) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    // Validate unique fields
                    if (profileRequest.getUsername() != null && !profileRequest.getUsername().equals(user.getUsername())) {
                        if (userRepository.existsByUsername(profileRequest.getUsername())) {
                            throw new IllegalArgumentException("Username already taken");
                        }
                        user.setUsername(profileRequest.getUsername());
                    }

                    if (profileRequest.getEmail() != null && !profileRequest.getEmail().equals(user.getEmail())) {
                        if (userRepository.existsByEmail(profileRequest.getEmail())) {
                            throw new IllegalArgumentException("Email already in use");
                        }
                        user.setEmail(profileRequest.getEmail());
                    }

                    if (profileRequest.getPhoneNumber() != null && !profileRequest.getPhoneNumber().equals(user.getPhoneNumber())) {
                        if (userRepository.existsByPhoneNumber(profileRequest.getPhoneNumber())) {
                            throw new IllegalArgumentException("Phone number already in use");
                        }
                        user.setPhoneNumber(profileRequest.getPhoneNumber());
                    } else if (profileRequest.getPhoneNumber() != null) {
                        user.setPhoneNumber(profileRequest.getPhoneNumber());
                    }

                    // Update other fields
                    if (profileRequest.getFirstName() != null) {
                        user.setFirstName(profileRequest.getFirstName());
                    }

                    if (profileRequest.getLastName() != null) {
                        user.setLastName(profileRequest.getLastName());
                    }

                    if (profileRequest.getProfilePicture() != null) {
                        user.setProfileImage(profileRequest.getProfilePicture());
                    }

                    // Handle location if present
                    if (profileRequest.getLocation() != null) {
                        UserProfileRequest.LocationDTO locationDTO = profileRequest.getLocation();
                        user.setLocationType(locationDTO.getType());

                        if (locationDTO.getCoordinates() != null && locationDTO.getCoordinates().length == 2) {
                            user.setLongitude(locationDTO.getCoordinates()[0]);
                            user.setLatitude(locationDTO.getCoordinates()[1]);
                        }

                        if (locationDTO.getAddress() != null) {
                            user.setAddress(locationDTO.getAddress());
                        }
                    } else if (profileRequest.getAddress() != null) {
                        user.setAddress(profileRequest.getAddress());
                    }

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
                user.getProfileImage(),
                user.getAddress(),
                user.getLocationType(),
                user.getLatitude(),
                user.getLongitude(),
                user.isEnabled(),
                user.isDisabled(),
                user.isDeleted(),
                user.isVerified(),
                user.getIdentificationNumber(),
                user.getVehicleNumber(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles().stream()
                        .map(role -> role.getName())
                        .toList()
        );
    }

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAllUsers()
                .stream()
                .map(this::mapToUserProfileResponse)
                .toList();
    }

    public UserProfileResponse createUserByAdmin(SignupRequest signUpRequest) {
        // Validate unique fields
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        if (signUpRequest.getPhoneNumber() != null && !signUpRequest.getPhoneNumber().isEmpty() &&
                userRepository.existsByPhoneNumber(signUpRequest.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        if (signUpRequest.getIdentificationNumber() != null && !signUpRequest.getIdentificationNumber().isEmpty() &&
                userRepository.existsByIdentificationNumber(signUpRequest.getIdentificationNumber())) {
            throw new IllegalArgumentException("Identification number is already registered");
        }

        // Create new user account
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        // Handle user details
        user.setFirstName(signUpRequest.getFirstName() != null ? signUpRequest.getFirstName() : "");
        user.setLastName(signUpRequest.getLastName() != null ? signUpRequest.getLastName() : "");
        user.setPhoneNumber(signUpRequest.getPhoneNumber() != null ? signUpRequest.getPhoneNumber() : "");
        user.setProfileImage(signUpRequest.getProfilePicture() != null ? signUpRequest.getProfilePicture() : "");
        user.setAddress(signUpRequest.getAddress() != null ? signUpRequest.getAddress() : "");

        // Handle location information
        if (signUpRequest.getLocation() != null) {
            user.setLocationType(signUpRequest.getLocation().getType());
            if (signUpRequest.getLocation().getCoordinates() != null) {
                user.setLongitude(signUpRequest.getLocation().getCoordinates()[0]);
                user.setLatitude(signUpRequest.getLocation().getCoordinates()[1]);
            } else {
                user.setLongitude(0.0);
                user.setLatitude(0.0);
            }
        } else {
            user.setLocationType("");
            user.setLongitude(0.0);
            user.setLatitude(0.0);
        }

        // Set status fields for admin-created accounts
        user.setDisabled(false);
        user.setDeleted(false);
        user.setVerified(true);  // Admin-created accounts are pre-verified
        user.setEnabled(false);

        // Special fields for driver or restaurant admin accounts if applicable
        if (signUpRequest.getIdentificationNumber() != null) {
            user.setIdentificationNumber(signUpRequest.getIdentificationNumber());
        }
        if (signUpRequest.getVehicleNumber() != null) {
            user.setVehicleNumber(signUpRequest.getVehicleNumber());
        }

        // Handle roles - default to CUSTOMER if none specified
        Set<Role> roles = assignUserRoles(signUpRequest.getRoles());
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Generate confirmation token and URL
        ConfirmationToken confirmationToken = new ConfirmationToken(savedUser);
        confirmationTokenRepository.save(confirmationToken);

        String confirmationUrl = CONFIRMATION_URL + confirmationToken.getToken();

        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .toList();

        // Send registration event
        UserRegistrationAdminEvent event = new UserRegistrationAdminEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                signUpRequest.getPassword(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                roleNames,
                "ADMIN_USER_REGISTERED",
                savedUser.getPhoneNumber(),
                confirmationUrl,
                System.currentTimeMillis()
        );

        kafkaProducerService.sendAdminUserRegistrationEvent(event);


        log.info("Admin created new user: {}, with roles: {}", savedUser.getUsername(),
                roles.stream().map(Role::getName).collect(Collectors.toList()));

        return mapToUserProfileResponse(savedUser);
    }

    private Set<Role> assignUserRoles(Set<String> requestedRoles) {
        Set<Role> roles = new HashSet<>();

        if (requestedRoles == null || requestedRoles.isEmpty()) {
            Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Error: Default role not found."));
            roles.add(customerRole);
            return roles;
        }

        for (String role : requestedRoles) {
            switch (role) {
                case "ROLE_ADMIN":
                    Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                            .orElseThrow(() -> new RuntimeException("Error: Admin role not found."));
                    roles.add(adminRole);
                    break;
                case "ROLE_RESTAURANT_ADMIN":
                    Role restaurantRole = roleRepository.findByName("ROLE_RESTAURANT_ADMIN")
                            .orElseThrow(() -> new RuntimeException("Error: Restaurant role not found."));
                    roles.add(restaurantRole);
                    break;
                case "ROLE_DRIVER":
                    Role deliveryRole = roleRepository.findByName("ROLE_DELIVERY_PERSONNEL")
                            .orElseThrow(() -> new RuntimeException("Error: Delivery role not found."));
                    roles.add(deliveryRole);
                    break;
                default:
                    Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                            .orElseThrow(() -> new RuntimeException("Error: Default role not found."));
                    roles.add(customerRole);
            }
        }
        return roles;
    }

    public UserProfileResponse updateUserByAdmin(Long userId, UpdateProfileRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Validate unique fields when they change (safely check for null values)
        if (updateRequest.getUsername() != null && !updateRequest.getUsername().equals(user.getUsername()) &&
                userRepository.existsByUsername(updateRequest.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(updateRequest.getEmail())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        if (updateRequest.getPhoneNumber() != null && !updateRequest.getPhoneNumber().isEmpty() &&
                !updateRequest.getPhoneNumber().equals(user.getPhoneNumber()) &&
                userRepository.existsByPhoneNumber(updateRequest.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        if (updateRequest.getIdentificationNumber() != null && !updateRequest.getIdentificationNumber().isEmpty() &&
                !updateRequest.getIdentificationNumber().equals(user.getIdentificationNumber()) &&
                userRepository.existsByIdentificationNumber(updateRequest.getIdentificationNumber())) {
            throw new IllegalArgumentException("Identification number is already registered");
        }

        // Update basic user information - only if provided in request
        if (updateRequest.getUsername() != null) {
            user.setUsername(updateRequest.getUsername());
        }

        if (updateRequest.getEmail() != null) {
            user.setEmail(updateRequest.getEmail());
        }

        // Only update password if provided
        if (updateRequest.getPassword() != null && !updateRequest.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateRequest.getPassword()));
        }

        // Update personal details
        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }

        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }

        if (updateRequest.getPhoneNumber() != null) {
            user.setPhoneNumber(updateRequest.getPhoneNumber());
        }

        if (updateRequest.getProfilePicture() != null) {
            user.setProfileImage(updateRequest.getProfilePicture());
        }

        if (updateRequest.getAddress() != null) {
            user.setAddress(updateRequest.getAddress());
        }

        // Update location information if provided
        if (updateRequest.getLocation() != null) {
            if (updateRequest.getLocation().getType() != null) {
                user.setLocationType(updateRequest.getLocation().getType());
            }

            if (updateRequest.getLocation().getCoordinates() != null &&
                    updateRequest.getLocation().getCoordinates().length == 2) {
                user.setLongitude(updateRequest.getLocation().getCoordinates()[0]);
                user.setLatitude(updateRequest.getLocation().getCoordinates()[1]);
            }
        }

        // Update special fields for drivers or restaurant admins
        if (updateRequest.getIdentificationNumber() != null) {
            user.setIdentificationNumber(updateRequest.getIdentificationNumber());
        }

        if (updateRequest.getVehicleNumber() != null) {
            user.setVehicleNumber(updateRequest.getVehicleNumber());
        }

        // Update status fields if present
        if (updateRequest.getDisabled() != null) {
            user.setDisabled(updateRequest.getDisabled());
        }

        if (updateRequest.getVerified() != null) {
            user.setVerified(updateRequest.getVerified());
        }

        // Update roles if specified
        if (updateRequest.getRoles() != null && !updateRequest.getRoles().isEmpty()) {
            Set<Role> roles = assignUserRoles(updateRequest.getRoles());
            user.setRoles(roles);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        log.info("Admin updated user: {}, with roles: {}", updatedUser.getUsername(),
                updatedUser.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));

        return mapToUserProfileResponse(updatedUser);
    }
}