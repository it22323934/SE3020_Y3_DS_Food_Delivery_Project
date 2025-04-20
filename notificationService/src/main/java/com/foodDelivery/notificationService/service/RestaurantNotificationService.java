package com.foodDelivery.notificationService.service;

import com.foodDelivery.notificationService.client.RestaurantServiceClient;
import com.foodDelivery.notificationService.client.UserServiceClient;
import com.foodDelivery.notificationService.dto.CuisineTypeResponse;
import com.foodDelivery.notificationService.dto.UserProfileResponse;
import com.foodDelivery.notificationService.emailTemplates.RestaurantEmailTemplates;
import com.foodDelivery.notificationService.interfaces.EmailService;
import com.foodDelivery.notificationService.interfaces.SmsService;
import com.foodDelivery.notificationService.modal.Notification;
import com.foodDelivery.notificationService.modal.NotificationStatus;
import com.foodDelivery.notificationService.modal.NotificationType;
import com.foodDelivery.notificationService.repository.NotificationRepository;
import com.foodDelivery.restaurantService.event.RestaurantEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantNotificationService {
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final RestaurantEmailTemplates restaurantEmailTemplates;
    @KafkaListener(topics = "restaurant-notifications", containerFactory = "kafkaListenerContainerFactoryBroker2")
    public void handleRestaurantEvent(RestaurantEvent event) {
        log.info("Received restaurant event: {} for restaurant: {}", event.getEventType(), event.getRestaurantName());

        try {
            switch (event.getEventType()) {
                case "RESTAURANT_CREATED" -> handleRestaurantCreated(event);
                case "RESTAURANT_UPDATED" -> handleRestaurantUpdated(event);
                default -> log.warn("Unknown restaurant event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing restaurant event: {}", e.getMessage(), e);
        }
    }

    private void handleRestaurantCreated(RestaurantEvent event) {
        log.info("Processing restaurant creation notification for restaurant: {}", event.getRestaurantId());

        // Collect cuisine names for the notification
        List<String> cuisineNames = resolveCuisineNames(event.getCuisineTypeIds());

        // Notify the restaurant's business email
        if (event.getRestaurantEmail() != null && !event.getRestaurantEmail().isEmpty()) {
            String emailContent = restaurantEmailTemplates.createRestaurantCreationEmail(event, cuisineNames);
            log.info("Sending creation email to restaurant email: {}", event.getRestaurantEmail());
            try {
                emailService.sendEmail(
                        event.getRestaurantEmail(),
                        "Your Restaurant Has Been Created: " + event.getRestaurantName(),
                        emailContent,
                        true
                );
                saveNotification(event.getRestaurantId(), null, emailContent,
                        NotificationType.EMAIL, event.getRestaurantEmail(), "RESTAURANT");
            } catch (Exception e) {
                log.error("Failed to send restaurant creation email: {}", e.getMessage(), e);
            }
        }

        // Send SMS to restaurant
        if (event.getRestaurantPhone() != null && !event.getRestaurantPhone().isEmpty()) {
            try {
                String smsContent = "Congratulations! Your restaurant '" + event.getRestaurantName() +
                        "' has been created on FlavorFleet. Log in to manage your restaurant, menu items, and orders.";
                log.info("Sending creation SMS to restaurant phone: {}", event.getRestaurantPhone());
                smsService.sendSms(event.getRestaurantPhone(), smsContent);
                saveNotification(event.getRestaurantId(), null, smsContent,
                        NotificationType.SMS, event.getRestaurantPhone(), "RESTAURANT");
            } catch (Exception e) {
                log.error("Failed to send restaurant creation SMS: {}", e.getMessage(), e);
            }
        }

        // Notify all admins
        if (event.getAdminIds() != null && !event.getAdminIds().isEmpty()) {
            log.info("Notifying {} restaurant admins: {}", event.getAdminIds().size(), event.getAdminIds());
            notifyAdmins(event.getAdminIds(), event.getRestaurantName(), cuisineNames, true);
        } else {
            log.warn("No admin IDs found for restaurant: {}", event.getRestaurantId());
        }
    }

    private void handleRestaurantUpdated(RestaurantEvent event) {
        List<String> cuisineNames = resolveCuisineNames(event.getCuisineTypeIds());

        // Notify new admins
        if (event.getAddedAdminIds() != null && !event.getAddedAdminIds().isEmpty()) {
            notifyAdmins(event.getAddedAdminIds(), event.getRestaurantName(), cuisineNames, true);
        }

        // Notify removed admins
        if (event.getRemovedAdminIds() != null && !event.getRemovedAdminIds().isEmpty()) {
            notifyAdmins(event.getRemovedAdminIds(), event.getRestaurantName(), cuisineNames, false);
        }

        // Notify restaurant about the update
        if (event.getRestaurantEmail() != null && !event.getRestaurantEmail().isEmpty()) {
            String emailContent = restaurantEmailTemplates.createRestaurantUpdateEmail(event, cuisineNames);
            emailService.sendEmail(
                    event.getRestaurantEmail(),
                    "Restaurant Updated: " + event.getRestaurantName(),
                    emailContent,
                    true
            );
            saveNotification(event.getRestaurantId(), null, emailContent,
                    NotificationType.EMAIL, event.getRestaurantEmail(), "RESTAURANT");
        }
    }

    private void notifyAdmins(List<String> adminIds, String restaurantName, List<String> cuisineNames, boolean isAdded) {
        if (adminIds == null || adminIds.isEmpty()) {
            log.warn("Admin IDs list is null or empty, skipping admin notifications");
            return;
        }

        log.info("Processing notifications for {} admins", adminIds.size());

        for (String adminId : adminIds) {
            if (adminId == null || adminId.trim().isEmpty()) {
                log.warn("Skipping null or empty admin ID");
                continue;
            }

            log.info("Getting user details for admin ID: {}", adminId);
            try {
                // Get user details from user service
                UserProfileResponse admin = userServiceClient.getUserById(adminId);

                if (admin == null) {
                    log.warn("User service returned null for admin ID: {}", adminId);
                    continue;
                }

                if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
                    log.warn("Admin {} has no email address", adminId);
                    continue;
                }

                log.info("Sending email notification to admin: {} <{}>", admin.getUsername(), admin.getEmail());

                // Send email notification
                String subject = isAdded ?
                        "You're Now an Admin for " + restaurantName :
                        "Admin Access Removed: " + restaurantName;

                String emailContent = isAdded ?
                        restaurantEmailTemplates.createAdminAddedEmail(admin.getUsername(), restaurantName, cuisineNames) :
                        restaurantEmailTemplates.createAdminRemovedEmail(admin.getUsername(), restaurantName);

                try {
                    emailService.sendEmail(admin.getEmail(), subject, emailContent, true);
                    saveNotification(adminId, null, emailContent, NotificationType.EMAIL, admin.getEmail(), "USER");
                    log.info("Successfully sent email to admin: {}", adminId);
                } catch (Exception e) {
                    log.error("Failed to send email to admin {}: {}", adminId, e.getMessage(), e);
                }

                // Send SMS if phone number available
                if (admin.getPhoneNumber() != null && !admin.getPhoneNumber().trim().isEmpty()) {
                    String smsContent = isAdded ?
                            "You have been added as an admin for restaurant '" + restaurantName + "'. Check your email for details." :
                            "Your admin access for restaurant '" + restaurantName + "' has been removed.";

                    try {
                        log.info("Sending SMS notification to admin: {} ({})", admin.getUsername(), admin.getPhoneNumber());
                        smsService.sendSms(admin.getPhoneNumber(), smsContent);
                        saveNotification(adminId, null, smsContent, NotificationType.SMS, admin.getPhoneNumber(), "USER");
                        log.info("Successfully sent SMS to admin: {}", adminId);
                    } catch (Exception e) {
                        log.error("Failed to send SMS to admin {}: {}", adminId, e.getMessage(), e);
                    }
                } else {
                    log.info("No phone number available for admin: {}, skipping SMS", adminId);
                }
            } catch (Exception e) {
                log.error("Error processing admin notification for {}: {}", adminId, e.getMessage(), e);
            }
        }
    }

    private List<String> resolveCuisineNames(List<String> cuisineIds) {
        if (cuisineIds == null || cuisineIds.isEmpty()) {
            return List.of("Not specified");
        }

        List<String> cuisineNames = new ArrayList<>();
        for (String cuisineId : cuisineIds) {
            try {
                CuisineTypeResponse cuisine = restaurantServiceClient.getCuisineTypeById(cuisineId);
                if (cuisine != null && cuisine.getName() != null) {
                    cuisineNames.add(cuisine.getName());
                }
            } catch (Exception e) {
                log.warn("Could not resolve cuisine type {}: {}", cuisineId, e.getMessage());
            }
        }

        return cuisineNames.isEmpty() ? List.of("Not specified") : cuisineNames;
    }

    private void saveNotification(String referenceId, String templateId, String content,
                                  NotificationType type, String recipient, String referenceType) {
        try {
            Notification notification = new Notification();
            notification.setReferenceId(referenceId);
            notification.setTemplateId(templateId);
            notification.setContent(content);
            notification.setType(type);
            notification.setStatus(NotificationStatus.SENT);
            notification.setReferenceType(referenceType);
            notification.setSentAt(LocalDateTime.now());

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Error saving notification: {}", e.getMessage(), e);
        }
    }
}