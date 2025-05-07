package com.foodDelivery.notificationService.service;

import com.foodDelivery.notificationService.client.RestaurantServiceClient;
import com.foodDelivery.notificationService.client.UserServiceClient;
import com.foodDelivery.notificationService.dto.CuisineTypeResponse;
import com.foodDelivery.notificationService.dto.RestaurantResponse;
import com.foodDelivery.notificationService.dto.UserProfileResponse;
import com.foodDelivery.notificationService.emailTemplates.RestaurantEmailTemplates;
import com.foodDelivery.notificationService.interfaces.EmailService;
import com.foodDelivery.notificationService.interfaces.SmsService;
import com.foodDelivery.notificationService.modal.Notification;
import com.foodDelivery.notificationService.modal.NotificationStatus;
import com.foodDelivery.notificationService.modal.NotificationType;
import com.foodDelivery.notificationService.repository.NotificationRepository;
import com.foodDelivery.restaurantService.event.PromotionCreatedEvent;
import com.foodDelivery.restaurantService.event.RestaurantEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final String RESTAURANT_TOPIC = "restaurant-notifications";
    private static final String PROMOTION_TOPIC = "promotion-notifications";

    @KafkaListener(topics = RESTAURANT_TOPIC, containerFactory = "kafkaListenerContainerFactoryBroker2")
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

    @KafkaListener(topics = PROMOTION_TOPIC, containerFactory = "kafkaListenerContainerFactoryBroker2")
    public void handlePromotionEvent(PromotionCreatedEvent event) {
        log.info("Received promotion event: {} for restaurant: {}", event.getEventType(), event.getRestaurantName());

        try {
            RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(event.getRestaurantId());
            if (restaurant != null && restaurant.getAdminIds() != null) {
                String emailContent = createPromotionEmail(event);
                notifyRestaurantAdmins(restaurant.getAdminIds(), emailContent, event);
            }
        } catch (Exception e) {
            log.error("Error processing promotion event: {}", e.getMessage(), e);
        }
    }

    private void notifyRestaurantAdmins(List<String> adminIds, String emailContent, PromotionCreatedEvent event) {
        String subject = switch (event.getEventType()) {
            case "PROMOTION_CREATED" -> "New Promotion Created: " + event.getCode();
            case "PROMOTION_UPDATED" -> "Promotion Updated: " + event.getCode();
            case "PROMOTION_DELETED" -> "Promotion Deleted: " + event.getCode();
            default -> "Promotion Notification: " + event.getCode();
        };

        for (String adminId : adminIds) {
            try {
                UserProfileResponse admin = userServiceClient.getUserById(adminId);
                if (admin != null && admin.getEmail() != null) {
                    emailService.sendEmail(admin.getEmail(), subject, emailContent, true);
                    saveNotification(event.getRestaurantId(), null, emailContent,
                            NotificationType.EMAIL, admin.getEmail(), "PROMOTION");
                    log.info("Sent {} notification to admin: {}", event.getEventType(), admin.getEmail());
                }
            } catch (Exception e) {
                log.error("Failed to notify admin {}: {}", adminId, e.getMessage());
            }
        }
    }

    private String createPromotionEmail(PromotionCreatedEvent event) {
        String title = switch (event.getEventType()) {
            case "PROMOTION_CREATED" -> "New Promotion Created";
            case "PROMOTION_UPDATED" -> "Promotion Updated";
            case "PROMOTION_DELETED" -> "Promotion Deleted";
            default -> "Promotion Notification";
        };

        String message = switch (event.getEventType()) {
            case "PROMOTION_CREATED" -> "A new promotion has been created for %s";
            case "PROMOTION_UPDATED" -> "Important changes have been made to promotion at %s";
            case "PROMOTION_DELETED" -> "A promotion has been deleted for %s";
            default -> "Promotion notification for %s";
        };

        return switch (event.getEventType()) {
            case "PROMOTION_UPDATED" -> createUpdatedPromotionEmail(event, title, message);
            case "PROMOTION_DELETED" -> createDeletedPromotionEmail(event, title, message);
            default -> createDetailedPromotionEmail(event, title, message);
        };
    }

    private String createUpdatedPromotionEmail(PromotionCreatedEvent event, String title, String message) {
        return """
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background-color: #fff3cd; padding: 20px; border-radius: 10px; margin-bottom: 20px; border: 1px solid #ffeeba;">
            <h1 style="color: #856404; margin-bottom: 10px;">%s</h1>
            <p style="color: #856404; margin-bottom: 20px;">%s</p>
        </div>
        
        <div style="background-color: #ffffff; border: 2px solid #17a2b8; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
            <h2 style="color: #17a2b8; margin-bottom: 15px; border-bottom: 2px solid #17a2b8; padding-bottom: 10px;">
                Updated Promotion Details
            </h2>
            
            <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin-bottom: 15px;">
                <div style="margin-bottom: 10px;">
                    <strong style="color: #17a2b8;">Promotion Code:</strong>
                    <span style="color: #17a2b8; font-size: 18px; font-weight: bold; display: block; margin-top: 5px;">%s</span>
                </div>
                <div style="margin-bottom: 10px;">
                    <strong style="color: #17a2b8;">Updated Description:</strong>
                    <p style="color: #495057; margin: 5px 0; font-style: italic;">%s</p>
                </div>
            </div>
            
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
                <div style="background-color: #e3f2fd; padding: 15px; border-radius: 5px;">
                    <strong style="color: #17a2b8;">Discount Rate:</strong>
                    <p style="color: #0056b3; font-size: 20px; font-weight: bold; margin: 5px 0;">%.1f%%</p>
                </div>
                <div style="background-color: #e3f2fd; padding: 15px; border-radius: 5px;">
                    <strong style="color: #17a2b8;">Maximum Discount:</strong>
                    <p style="color: #0056b3; font-size: 20px; font-weight: bold; margin: 5px 0;">$%.2f</p>
                </div>
            </div>
            
            <div style="background-color: #e3f2fd; padding: 15px; border-radius: 5px; margin-bottom: 20px;">
                <strong style="color: #17a2b8;">Minimum Order Required:</strong>
                <p style="color: #0056b3; font-size: 18px; font-weight: bold; margin: 5px 0;">$%.2f</p>
            </div>
            
            <div style="border-top: 1px solid #dee2e6; padding-top: 15px;">
                <h3 style="color: #17a2b8; margin-bottom: 10px;">Validity Period</h3>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                    <div style="background-color: #f8f9fa; padding: 10px; border-radius: 5px;">
                        <strong style="color: #17a2b8;">Starts From:</strong>
                        <p style="color: #495057; margin: 5px 0;">%s</p>
                    </div>
                    <div style="background-color: #f8f9fa; padding: 10px; border-radius: 5px;">
                        <strong style="color: #17a2b8;">Valid Until:</strong>
                        <p style="color: #495057; margin: 5px 0;">%s</p>
                    </div>
                </div>
            </div>
        </div>
        
        <div style="background-color: #cce5ff; padding: 15px; border-radius: 5px; text-align: center; border: 1px solid #b8daff;">
            <p style="color: #004085; margin: 0;">
                This promotion has been updated. Please review the changes carefully.
                <br>This is an automated message. Please do not reply.
            </p>
        </div>
    </div>
    """.formatted(
                title,
                String.format(message, event.getRestaurantName()),
                event.getCode(),
                event.getDescription(),
                event.getDiscountPercentage(),
                event.getMaxDiscount(),
                event.getMinOrderAmount(),
                event.getStartDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                event.getEndDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
        );
    }

    private String createDetailedPromotionEmail(PromotionCreatedEvent event, String title, String message) {
        return """
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
            <h1 style="color: #2c3e50; margin-bottom: 10px;">%s</h1>
            <p style="color: #7f8c8d; margin-bottom: 20px;">%s</p>
        </div>
        
        <div style="background-color: #ffffff; border: 1px solid #e1e1e1; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
            <h2 style="color: #e74c3c; margin-bottom: 15px;">Promotion Details</h2>
            <div style="margin-bottom: 15px;">
                <strong style="color: #2c3e50;">Code:</strong>
                <span style="color: #e74c3c; font-size: 18px; font-weight: bold;">%s</span>
            </div>
            <div style="margin-bottom: 15px;">
                <strong style="color: #2c3e50;">Description:</strong>
                <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 15px;">
                <div>
                    <strong style="color: #2c3e50;">Discount:</strong>
                    <p style="color: #27ae60; font-weight: bold; margin: 5px 0;">%.1f%%</p>
                </div>
                <div>
                    <strong style="color: #2c3e50;">Max Discount:</strong>
                    <p style="color: #27ae60; font-weight: bold; margin: 5px 0;">$%.2f</p>
                </div>
            </div>
            <div style="margin-bottom: 15px;">
                <strong style="color: #2c3e50;">Minimum Order:</strong>
                <p style="color: #7f8c8d; margin: 5px 0;">$%.2f</p>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                <div>
                    <strong style="color: #2c3e50;">Start Date:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
                </div>
                <div>
                    <strong style="color: #2c3e50;">End Date:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
                </div>
            </div>
        </div>
        
        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; text-align: center;">
            <p style="color: #7f8c8d; margin: 0;">This is an automated message. Please do not reply.</p>
        </div>
    </div>
    """.formatted(
                title,
                String.format(message, event.getRestaurantName()),
                event.getCode(),
                event.getDescription(),
                event.getDiscountPercentage(),
                event.getMaxDiscount(),
                event.getMinOrderAmount(),
                event.getStartDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                event.getEndDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
        );
    }

    private String createDeletedPromotionEmail(PromotionCreatedEvent event, String title, String message) {
        return """
    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
            <h1 style="color: #2c3e50; margin-bottom: 10px;">%s</h1>
            <p style="color: #7f8c8d; margin-bottom: 20px;">%s</p>
        </div>
        
        <div style="background-color: #ffffff; border: 1px solid #e1e1e1; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
            <h2 style="color: #e74c3c; margin-bottom: 15px;">Deleted Promotion</h2>
            <div style="margin-bottom: 15px;">
                <strong style="color: #2c3e50;">Code:</strong>
                <span style="color: #e74c3c; font-size: 18px; font-weight: bold;">%s</span>
            </div>
        </div>
        
        <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; text-align: center;">
            <p style="color: #7f8c8d; margin: 0;">This is an automated message. Please do not reply.</p>
        </div>
    </div>
    """.formatted(
                title,
                String.format(message, event.getRestaurantName()),
                event.getCode()
        );
    }
}