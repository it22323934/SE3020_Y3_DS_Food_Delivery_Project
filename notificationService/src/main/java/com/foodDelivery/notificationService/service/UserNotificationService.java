// notificationService/src/main/java/com/foodDelivery/notificationService/service/NotificationService.java
package com.foodDelivery.notificationService.service;

import com.foodDelivery.notificationService.event.OrderStatusEvent;
import com.foodDelivery.userService.event.PasswordResetEvent;
import com.foodDelivery.userService.event.UserRegistrationAdminEvent;
import com.foodDelivery.userService.event.UserRegistrationEvent;
import com.foodDelivery.notificationService.modal.*;
import com.foodDelivery.notificationService.repository.NotificationRepository;
import com.foodDelivery.notificationService.repository.NotificationTemplateRepository;
import com.foodDelivery.userService.modal.UserNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(UserNotificationService.class);
    private final JavaMailSender javaMailSender;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    private final SMSService smsService;
    private final PushNotificationService pushService;

    public UserNotificationService(JavaMailSender javaMailSender,
                                   NotificationTemplateRepository templateRepository,
                                   NotificationRepository notificationRepository,
                                   SMSService smsService,
                                   PushNotificationService pushService) {
        this.javaMailSender = javaMailSender;
        this.templateRepository = templateRepository;
        this.notificationRepository = notificationRepository;
        this.smsService = smsService;
        this.pushService = pushService;
    }

    @KafkaListener(topics = "user-registration", groupId = "notification-service")
    public void handleUserRegistration(UserRegistrationEvent event) {
        logger.info("Received user registration event: {}", event);

        if(Objects.equals(event.getEventType(), "GOOGLE_USER_REGISTERED")){
            sendGoogleEmailNotification(event);
        }
        if(Objects.equals(event.getEventType(), "USER_REGISTERED")){
            sendEmailNotification(event);
        }

        // Send SMS notification if phone number exists
        if (event.getPhoneNumber() != null && !event.getPhoneNumber().isEmpty()) {
            sendSmsNotification(event);
        }
    }

    @KafkaListener(topics = "user-password-reset", groupId = "notification-service")
    public void handlePasswordReset(PasswordResetEvent event) {
        logger.info("Received password reset event for user: {}", event.getEmail());

        // Create notification record
        Notification notification = new Notification();
        notification.setUserId(String.valueOf(event.getUserId()));
        notification.setType(NotificationType.EMAIL);
        notification.setReferenceType("USER");
        notification.setReferenceId(String.valueOf(event.getUserId()));

        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.EMAIL, EventType.PASSWORD_RESET_REQUESTED);

        if (template.isPresent()) {
            notification.setTemplateId(template.get().getTemplateId());

            Map<String, String> variables = new HashMap<>();
            variables.put("firstName", event.getFirstName());
            variables.put("resetUrl", event.getResetUrl());

            String content = processTemplate(template.get().getContent(), variables);
            notification.setContent(content);

            sendEmailNotification(event.getEmail(), template.get().getSubject(), content);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            // Use default template if none found
            MimeMessagePreparator messagePreparatory = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
                messageHelper.setFrom("asirijayawardena920@gmail.com");
                messageHelper.setTo(event.getEmail());
                messageHelper.setSubject("Reset Your Password");
                messageHelper.setText(String.format("""
                Dear %s,
                
                You have requested to reset your password. Please click the link below to set a new password:
                
                %s
                
                This link will expire in 24 hours. If you didn't request this, please ignore this email.
                
                Best Regards,
                Food Delivery Team
                """,
                        event.getFirstName(),
                        event.getResetUrl()), true);
            };

            try {
                javaMailSender.send(messagePreparatory);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                logger.info("Password reset email sent to {}", event.getEmail());
            } catch (MailException e) {
                notification.setStatus(NotificationStatus.FAILED);
                logger.error("Failed to send password reset email", e);
            }
        }

        notificationRepository.save(notification);
    }

    @KafkaListener(topics = "order-status-change", groupId = "notification-service")
    public void handleOrderStatusChange(OrderStatusEvent event) {
        logger.info("Received order status change event: {}", event);

        // Send email notification
        sendEmailForOrderStatus(event);

        // Send SMS notification
        sendSmsForOrderStatus(event);

        // Send push notification
        sendPushForOrderStatus(event);
    }

    @KafkaListener(topics = "admin-user-registration", groupId = "notification-service")
    public void handleAdminUserRegistration(UserRegistrationAdminEvent event) {
        logger.info("Received admin-created user registration event: {}", event);

        // Create notification record
        Notification notification = new Notification();
        notification.setUserId(String.valueOf(event.getUserId()));
        notification.setType(NotificationType.EMAIL);
        notification.setReferenceType("USER");
        notification.setReferenceId(String.valueOf(event.getUserId()));

        // Format roles for display
        String rolesDisplay = event.getRoles() != null ?
                String.join(", ", event.getRoles().stream()
                        .map(role -> role.replace("ROLE_", ""))
                        .collect(java.util.stream.Collectors.toList())) :
                "CUSTOMER";

        // Look for admin-specific template
        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.EMAIL, EventType.ADMIN_USER_REGISTRATION);

        if (template.isPresent()) {
            notification.setTemplateId(template.get().getTemplateId());

            // Replace variables in template
            String content = template.get().getContent()
                    .replace("{{firstName}}", event.getFirstName())
                    .replace("{{lastName}}", event.getLastName())
                    .replace("{{username}}", event.getUsername())
                    .replace("{{password}}", event.getPassword())
                    .replace("{{verificationLink}}", event.getConfirmationUrl())
                    .replace("{{roles}}", rolesDisplay);

            notification.setContent(content);
            sendEmailNotification(event.getEmail(), template.get().getSubject(), content);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            // Fallback to default HTML template for admin-created accounts
            String htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body {
                    font-family: 'Arial', sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                }
                .header {
                    background-color: #FF4500;
                    color: white;
                    padding: 20px;
                    text-align: center;
                    border-radius: 5px 5px 0 0;
                }
                .content {
                    padding: 20px;
                    background-color: #fff;
                    border-left: 1px solid #ddd;
                    border-right: 1px solid #ddd;
                }
                .footer {
                    background-color: #f4f4f4;
                    padding: 15px;
                    text-align: center;
                    font-size: 12px;
                    color: #666;
                    border-radius: 0 0 5px 5px;
                    border: 1px solid #ddd;
                }
                .button {
                    background-color: #FF4500;
                    color: white;
                    padding: 10px 20px;
                    text-align: center;
                    text-decoration: none;
                    display: inline-block;
                    border-radius: 5px;
                    font-weight: bold;
                }
                .credentials {
                    background-color: #f9f9f9;
                    border: 1px solid #ddd;
                    padding: 15px;
                    margin: 20px 0;
                    border-radius: 5px;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>Your FlavorFleet Account</h1>
            </div>
            <div class="content">
                <p>Dear %s,</p>
                <p>An administrator has created an account for you on FlavorFleet with the role(s): <strong>%s</strong>.</p>

                <div class="credentials">
                    <p><strong>Your login credentials:</strong></p>
                    <p>Username: <strong>%s</strong></p>
                    <p>Password: <strong>%s</strong></p>
                    <p><em>For security, please change your password after first login.</em></p>
                </div>

                <p>Before you can use your account, please verify your email address by clicking the button below:</p>

                <p style="text-align: center; margin-top: 25px;">
                    <a href="%s" class="button">Verify Email Address</a>
                </p>

                <p>If the button doesn't work, copy and paste this link into your browser:</p>
                <p style="word-break: break-all;">%s</p>

                <p>After verification, you can enjoy all FlavorFleet features based on your assigned role.</p>
            </div>
            <div class="footer">
                <p>&copy; %d FlavorFleet. All rights reserved.</p>
                <p>If you did not expect this account, please contact our support at <a href="mailto:support@FlavorFleet.com">support@FlavorFleet.com</a></p>
            </div>
        </body>
        </html>
        """.formatted(
                    event.getFirstName(),
                    rolesDisplay,
                    event.getUsername(),
                    event.getPassword(),
                    event.getConfirmationUrl(),
                    event.getConfirmationUrl(),
                    java.time.Year.now().getValue()
            );

            MimeMessagePreparator messagePreparatory = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
                messageHelper.setFrom("noreply@FlavorFleet.com", "FlavorFleet");
                messageHelper.setTo(event.getEmail());
                messageHelper.setSubject("Your FlavorFleet Account - Email Verification Required");
                messageHelper.setText(htmlContent, true);
            };

            try {
                javaMailSender.send(messagePreparatory);
                notification.setContent(htmlContent);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                logger.info("Admin-created account email sent to: {}", event.getEmail());
            } catch (MailException e) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setContent("Failed to send email: " + e.getMessage());
                logger.error("Failed to send admin-created account email", e);
            }
        }

        notificationRepository.save(notification);

        // Send SMS notification if phone number exists
        if (event.getPhoneNumber() != null && !event.getPhoneNumber().isEmpty()) {
            sendSmsNotificationForAdminCreatedAccount(event);
        }
    }

    @KafkaListener(topics = "user-profile-update", groupId = "notification-service")
    public void handleProfileUpdate(UserNotificationEvent event) {
        logger.info("Received profile update event for user: {}", event.getEmail());

        Map<String, Object> eventData = event.getData();
        Integer userId = (Integer) eventData.get("userId");
        String username = (String) eventData.get("username");
        String email = (String) eventData.get("email");

        // Create notification record
        Notification notification = new Notification();
        notification.setUserId(userId.toString());
        notification.setType(NotificationType.EMAIL);
        notification.setReferenceType("USER");
        notification.setReferenceId(userId.toString());

        // Retrieve changed fields
        @SuppressWarnings("unchecked")
        Map<String, String> changedFields = (Map<String, String>) eventData.get("changedFields");

        // Look for profile update template
        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.EMAIL, EventType.PROFILE_UPDATED_BY_ADMIN);

        if (template.isPresent()) {
            notification.setTemplateId(template.get().getTemplateId());

            // Build changed fields list for template
            StringBuilder changesHtml = new StringBuilder("<ul>");
            changedFields.forEach((field, value) -> {
                changesHtml.append("<li><strong>").append(field).append("</strong>: ")
                        .append(field.equals("Password") ? "******" : value)
                        .append("</li>");
            });
            changesHtml.append("</ul>");

            // Replace variables in template
            Map<String, String> variables = new HashMap<>();
            variables.put("username", username);
            variables.put("firstName", (String) eventData.get("firstName"));
            variables.put("lastName", (String) eventData.get("lastName"));
            variables.put("changedFields", changesHtml.toString());

            String content = processTemplate(template.get().getContent(), variables);
            notification.setContent(content);

            sendEmailNotification(email, template.get().getSubject(), content);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            // Use default HTML template if none found
            String changesHtml = formatChangedFieldsHtml(changedFields);

            String htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body {
                    font-family: 'Arial', sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                }
                .header {
                    background-color: #FF4500;
                    color: white;
                    padding: 20px;
                    text-align: center;
                    border-radius: 5px 5px 0 0;
                }
                .content {
                    padding: 20px;
                    background-color: #fff;
                    border-left: 1px solid #ddd;
                    border-right: 1px solid #ddd;
                }
                .changes {
                    background-color: #f9f9f9;
                    border: 1px solid #eee;
                    padding: 15px;
                    margin: 20px 0;
                    border-radius: 5px;
                }
                .footer {
                    background-color: #f4f4f4;
                    padding: 15px;
                    text-align: center;
                    font-size: 12px;
                    color: #666;
                    border-radius: 0 0 5px 5px;
                    border: 1px solid #ddd;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>Account Update Notification</h1>
            </div>
            <div class="content">
                <p>Dear %s,</p>
                <p>We're writing to inform you that an administrator has updated your FlavorFleet account.</p>
                
                <div class="changes">
                    <p><strong>The following changes were made to your account:</strong></p>
                    %s
                </div>
                
                <p>If you have any questions about these changes, please contact customer support.</p>
            </div>
            <div class="footer">
                <p>&copy; %d FlavorFleet. All rights reserved.</p>
                <p>If you didn't authorize these changes, please contact our support at <a href="mailto:support@FlavorFleet.com">support@FlavorFleet.com</a></p>
            </div>
        </body>
        </html>
        """.formatted(
                    username,
                    changesHtml,
                    java.time.Year.now().getValue()
            );

            MimeMessagePreparator messagePreparator = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
                messageHelper.setFrom("noreply@FlavorFleet.com", "FlavorFleet");
                messageHelper.setTo(email);
                messageHelper.setSubject("Your FlavorFleet Account Has Been Updated");
                messageHelper.setText(htmlContent, true);
            };

            try {
                javaMailSender.send(messagePreparator);
                notification.setContent(htmlContent);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                logger.info("Profile update notification email sent to: {}", email);
            } catch (MailException e) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setContent("Failed to send email: " + e.getMessage());
                logger.error("Failed to send profile update notification email", e);
            }
        }

        notificationRepository.save(notification);

        // If phone number exists, send SMS notification
        String phoneNumber = (String) eventData.get("phoneNumber");
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            sendProfileUpdateSMS(userId.toString(), phoneNumber, username, changedFields);
        }
    }

    private String formatChangedFieldsHtml(Map<String, String> changedFields) {
        StringBuilder html = new StringBuilder("<ul>");
        changedFields.forEach((field, value) -> {
            html.append("<li><strong>")
                    .append(field)
                    .append("</strong>: ")
                    .append(field.equals("Password") ? "******" : value)
                    .append("</li>");
        });
        html.append("</ul>");
        return html.toString();
    }

    private void sendProfileUpdateSMS(String userId, String phoneNumber, String username, Map<String, String> changedFields) {
        try {
            // Format phone number properly (ensure it starts with +)
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+" + phoneNumber;
            }

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType(NotificationType.SMS);
            notification.setReferenceType("USER");
            notification.setReferenceId(userId);

            Optional<NotificationTemplate> template = templateRepository
                    .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.SMS, EventType.PROFILE_UPDATED_BY_ADMIN);

            String content;
            if (template.isPresent()) {
                Map<String, String> variables = new HashMap<>();
                variables.put("username", username);

                StringBuilder changesText = new StringBuilder();
                int count = 0;
                for (Map.Entry<String, String> entry : changedFields.entrySet()) {
                    if (count > 0) changesText.append(", ");
                    changesText.append(entry.getKey());
                    count++;
                }
                variables.put("changedFieldsList", changesText.toString());

                content = processTemplate(template.get().getContent(), variables);
                notification.setTemplateId(template.get().getTemplateId());
            } else {
                // Default SMS message
                content = String.format("FlavorFleet: Your account has been updated by an administrator. " +
                                "Fields updated: %s. Check your email for details.",
                        String.join(", ", changedFields.keySet()));
            }

            notification.setContent(content);
            smsService.sendSMS(phoneNumber, content);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            logger.info("Profile update SMS sent to {}", phoneNumber);

            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Failed to send profile update SMS: {}", e.getMessage(), e);

            // Create a failed notification record
            Notification failedNotification = new Notification();
            failedNotification.setUserId(userId);
            failedNotification.setType(NotificationType.SMS);
            failedNotification.setReferenceType("USER");
            failedNotification.setReferenceId(userId);
            failedNotification.setContent("Failed to send SMS: " + e.getMessage());
            failedNotification.setStatus(NotificationStatus.FAILED);
            failedNotification.setSentAt(LocalDateTime.now());

            notificationRepository.save(failedNotification);
        }
    }

    public void sendGoogleEmailNotification(UserRegistrationEvent event) {
        logger.info("Preparing welcome email for Google authenticated user: {}", event.getEmail());

        // Create notification record
        Notification notification = new Notification();
        notification.setUserId(String.valueOf(event.getUserId()));
        notification.setType(NotificationType.EMAIL);
        notification.setReferenceType("USER");
        notification.setReferenceId(String.valueOf(event.getUserId()));

        // Check if we have a template for Google signups
        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.EMAIL, EventType.GOOGLE_USER_REGISTRATION);

        if (template.isPresent()) {
            notification.setTemplateId(template.get().getTemplateId());

            // Replace variables in template
            String content = template.get().getContent()
                    .replace("{{firstName}}", event.getFirstName())
                    .replace("{{lastName}}", event.getLastName());

            notification.setContent(content);
            sendEmailNotification(event.getEmail(), template.get().getSubject(), content);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            // Use default HTML template for Google users
            String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: 'Arial', sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                    }
                    .header {
                        background-color: #FF4500;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }
                    .content {
                        padding: 20px;
                        background-color: #fff;
                        border-left: 1px solid #ddd;
                        border-right: 1px solid #ddd;
                    }
                    .footer {
                        background-color: #f4f4f4;
                        padding: 15px;
                        text-align: center;
                        font-size: 12px;
                        color: #666;
                        border-radius: 0 0 5px 5px;
                        border: 1px solid #ddd;
                    }
                    .button {
                        background-color: #FF4500;
                        color: white;
                        padding: 10px 20px;
                        text-align: center;
                        text-decoration: none;
                        display: inline-block;
                        border-radius: 5px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Welcome to FlavorFleet!</h1>
                </div>
                <div class="content">
                    <p>Dear %s,</p>
                    <p>Thank you for signing up with your Google account. Your registration is complete and your account is ready to use!</p>
                    <p>With FlavorFleet you can:</p>
                    <ul>
                        <li>Browse restaurants and menus</li>
                        <li>Place orders quickly and securely</li>
                        <li>Track your delivery in real-time</li>
                        <li>Save your favorite restaurants and meals</li>
                    </ul>
                    <p style="text-align: center; margin-top: 25px;">
                        <a href="https://FlavorFleet.com/browse" class="button">Start Ordering</a>
                    </p>
                </div>
                <div class="footer">
                    <p>&copy; %d FlavorFleet. All rights reserved.</p>
                    <p>If you did not create this account, please contact our support at <a href="mailto:support@FlavorFleet.com">support@FlavorFleet.com</a></p>
                </div>
            </body>
            </html>
            """.formatted(event.getFirstName(), java.time.Year.now().getValue());

            MimeMessagePreparator messagePreparatory = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
                messageHelper.setFrom("noreply@FlavorFleet.com", "FlavorFleet");
                messageHelper.setTo(event.getEmail());
                messageHelper.setSubject("Welcome to FlavorFleet - Your Account is Ready!");
                messageHelper.setText(htmlContent, true);
            };

            try {
                javaMailSender.send(messagePreparatory);
                notification.setContent(htmlContent);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                logger.info("Welcome email sent to Google user: {}", event.getEmail());
            } catch (MailException e) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setContent("Failed to send email: " + e.getMessage());
                logger.error("Failed to send Google welcome email", e);
            }
        }

        notificationRepository.save(notification);
    }

    public void sendEmailNotification(UserRegistrationEvent event) {
        logger.info("Received user registration event: {}", event);

        // Create notification record
        Notification notification = new Notification();
        notification.setUserId(String.valueOf(event.getUserId()));
        notification.setType(NotificationType.EMAIL);
        notification.setReferenceType("USER");
        notification.setReferenceId(String.valueOf(event.getUserId()));

        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.EMAIL, EventType.USER_REGISTRATION);

        if (template.isPresent()) {
            notification.setTemplateId(template.get().getTemplateId());

            // Replace variables in template
            String content = template.get().getContent()
                    .replace("{{firstName}}", event.getFirstName())
                    .replace("{{lastName}}", event.getLastName())
                    .replace("{{confirmationUrl}}", event.getConfirmationUrl());

            notification.setContent(content);
            sendEmailNotification(event.getEmail(), template.get().getSubject(), content);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } else {
            // Use default HTML template if none found
            String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: 'Arial', sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                    }
                    .header {
                        background-color: #FF4500;
                        color: white;
                        padding: 20px;
                        text-align: center;
                        border-radius: 5px 5px 0 0;
                    }
                    .content {
                        padding: 20px;
                        background-color: #fff;
                        border-left: 1px solid #ddd;
                        border-right: 1px solid #ddd;
                    }
                    .verification-box {
                        background-color: #f9f9f9;
                        border: 1px solid #eee;
                        padding: 15px;
                        margin: 20px 0;
                        text-align: center;
                        border-radius: 5px;
                    }
                    .footer {
                        background-color: #f4f4f4;
                        padding: 15px;
                        text-align: center;
                        font-size: 12px;
                        color: #666;
                        border-radius: 0 0 5px 5px;
                        border: 1px solid #ddd;
                    }
                    .button {
                        background-color: #FF4500;
                        color: white;
                        padding: 10px 20px;
                        text-align: center;
                        text-decoration: none;
                        display: inline-block;
                        border-radius: 5px;
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Welcome to FlavorFleet!</h1>
                </div>
                <div class="content">
                    <p>Dear %s %s,</p>
                    <p>Thank you for creating an account with FlavorFleet!</p>
                    
                    <div class="verification-box">
                        <p><strong>Please verify your email address to activate your account</strong></p>
                        <p>You'll need to verify your email before you can place orders and enjoy our services.</p>
                        <p style="text-align: center; margin-top: 15px;">
                            <a href="%s" class="button">Verify My Email</a>
                        </p>
                    </div>
                    
                    <p>With FlavorFleet you can:</p>
                    <ul>
                        <li>Browse restaurants and menus</li>
                        <li>Place orders quickly and securely</li>
                        <li>Track your delivery in real-time</li>
                        <li>Save your favorite restaurants and meals</li>
                    </ul>
                    
                    <p>This verification link will expire in 24 hours.</p>
                </div>
                <div class="footer">
                    <p>&copy; %d FlavorFleet. All rights reserved.</p>
                    <p>If you did not create this account, please ignore this email or contact our support at <a href="mailto:support@FlavorFleet.com">support@FlavorFleet.com</a></p>
                </div>
            </body>
            </html>
            """.formatted(
                    event.getFirstName(),
                    event.getLastName(),
                    event.getConfirmationUrl(),
                    java.time.Year.now().getValue()
            );

            MimeMessagePreparator messagePreparatory = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
                messageHelper.setFrom("noreply@FlavorFleet.com", "FlavorFleet");
                messageHelper.setTo(event.getEmail());
                messageHelper.setSubject("Welcome to FlavorFleet - Please Verify Your Email");
                messageHelper.setText(htmlContent, true);
            };

            try {
                javaMailSender.send(messagePreparatory);
                notification.setContent(htmlContent);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                logger.info("Registration confirmation email sent to {}", event.getEmail());
            } catch (MailException e) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setContent("Failed to send email: " + e.getMessage());
                logger.error("Failed to send registration email", e);
            }
        }

        notificationRepository.save(notification);
    }

    private void sendSmsNotification(UserRegistrationEvent event) {
        try {
            // Validate phone number first
            if (event.getPhoneNumber() == null || event.getPhoneNumber().trim().isEmpty()) {
                logger.warn("Cannot send SMS: Phone number is empty");
                return;
            }

            // Format phone number properly (ensure it starts with +)
            String phoneNumber = event.getPhoneNumber().trim();
            if (!phoneNumber.startsWith("+")) {
                phoneNumber = "+" + phoneNumber;
            }

            Notification notification = new Notification();
            notification.setUserId(String.valueOf(event.getUserId()));
            notification.setType(NotificationType.SMS);
            notification.setReferenceType("USER");
            notification.setReferenceId(String.valueOf(event.getUserId()));

            Optional<NotificationTemplate> template = templateRepository
                    .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.SMS, EventType.USER_REGISTRATION);

            String content;
            if (template.isPresent()) {
                content = template.get().getContent()
                        .replace("{{firstName}}", event.getFirstName())
                        .replace("{{lastName}}", event.getLastName());
                notification.setTemplateId(template.get().getTemplateId());
            } else {
                // Default SMS message
                content = String.format("Welcome to Food Delivery, %s! Please check your email for a confirmation link.",
                        event.getFirstName());
            }

            notification.setContent(content);
            smsService.sendSMS(phoneNumber, content);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            logger.info("Registration SMS sent to {}", phoneNumber);

            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Failed to send registration SMS: {}", e.getMessage(), e);

            // Create a failed notification record
            Notification failedNotification = new Notification();
            failedNotification.setUserId(String.valueOf(event.getUserId()));
            failedNotification.setType(NotificationType.SMS);
            failedNotification.setReferenceType("USER");
            failedNotification.setReferenceId(String.valueOf(event.getUserId()));
            failedNotification.setContent("Failed to send SMS: " + e.getMessage());
            failedNotification.setStatus(NotificationStatus.FAILED);
            failedNotification.setSentAt(LocalDateTime.now());

            notificationRepository.save(failedNotification);
        }
    }

    private void sendSmsNotificationForAdminCreatedAccount(UserRegistrationAdminEvent event) {
        try {
            String phoneNumber = event.getPhoneNumber();

            Notification notification = new Notification();
            notification.setUserId(String.valueOf(event.getUserId()));
            notification.setType(NotificationType.SMS);
            notification.setReferenceType("USER");
            notification.setReferenceId(String.valueOf(event.getUserId()));

            Optional<NotificationTemplate> template = templateRepository
                    .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.SMS, EventType.ADMIN_USER_REGISTRATION);

            if (template.isPresent()) {
                notification.setTemplateId(template.get().getTemplateId());
                String content = template.get().getContent()
                        .replace("{{username}}", event.getUsername())
                        .replace("{{password}}", event.getPassword());

                notification.setContent(content);
                smsService.sendSMS(phoneNumber, content);
            } else {
                // Default SMS content
                String content = String.format(
                        "FlavorFleet: Your account has been created. Username: %s, Password: %s. Please verify your email to activate your account.",
                        event.getUsername(),
                        event.getPassword()
                );

                notification.setContent(content);
                smsService.sendSMS(phoneNumber, content);
            }

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            logger.info("Admin user registration SMS sent to {}", phoneNumber);

            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Failed to send admin user registration SMS: {}", e.getMessage(), e);

            // Create a failed notification record
            Notification failedNotification = new Notification();
            failedNotification.setUserId(String.valueOf(event.getUserId()));
            failedNotification.setType(NotificationType.SMS);
            failedNotification.setReferenceType("USER");
            failedNotification.setReferenceId(String.valueOf(event.getUserId()));
            failedNotification.setContent("Failed to send SMS: " + e.getMessage());
            failedNotification.setStatus(NotificationStatus.FAILED);
            failedNotification.setSentAt(LocalDateTime.now());

            notificationRepository.save(failedNotification);
        }
    }

    private void sendEmailForOrderStatus(OrderStatusEvent event) {
        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.EMAIL,
                        EventType.valueOf(event.getStatus()));

        if (template.isPresent()) {
            String content = processTemplate(template.get().getContent(), createOrderVariables(event));
            sendEmailNotification(event.getCustomerEmail(), template.get().getSubject(), content);

            saveNotification(event.getCustomerId(), template.get().getTemplateId(),
                    content, NotificationType.EMAIL, event.getOrderId().toString(), "ORDER");
        } else {
            // Default email for order status
            String subject = "Your Order #" + event.getOrderId() + " - " + event.getStatus();
            String content = String.format("Dear %s, your order #%d status has been updated to %s.",
                    event.getCustomerName(), event.getOrderId(), event.getStatus());

            sendEmailNotification(event.getCustomerEmail(), subject, content);

            saveNotification(event.getCustomerId(), null, content, NotificationType.EMAIL,
                    event.getOrderId().toString(), "ORDER");
        }
    }

    private void sendSmsForOrderStatus(OrderStatusEvent event) {
        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.SMS,
                        EventType.valueOf(event.getStatus()));

        if (template.isPresent() && event.getCustomerPhone() != null) {
            String content = processTemplate(template.get().getContent(), createOrderVariables(event));
            smsService.sendSMS(event.getCustomerPhone(), content);

            saveNotification(event.getCustomerId(), template.get().getTemplateId(),
                    content, NotificationType.SMS, event.getOrderId().toString(), "ORDER");
        }
    }

    private void sendPushForOrderStatus(OrderStatusEvent event) {
        Optional<NotificationTemplate> template = templateRepository
                .findByTypeAndEventTypeAndIsActiveTrue(NotificationType.PUSH,
                        EventType.valueOf(event.getStatus()));

        if (template.isPresent()) {
            String content = processTemplate(template.get().getContent(), createOrderVariables(event));
            pushService.sendPushNotification(event.getCustomerId(), template.get().getSubject(), content);

            saveNotification(event.getCustomerId(), template.get().getTemplateId(),
                    content, NotificationType.PUSH, event.getOrderId().toString(), "ORDER");
        }
    }

    private void sendEmailNotification(String to, String subject, String content) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("fooddelivery@email.com");
            messageHelper.setTo(to);
            messageHelper.setSubject(subject);
            messageHelper.setText(content, true);
        };

        try {
            javaMailSender.send(messagePreparator);
            logger.info("Email sent to {}", to);
        } catch (MailException e) {
            logger.error("Failed to send email", e);
        }
    }

    private String processTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private Map<String, String> createOrderVariables(OrderStatusEvent event) {
        Map<String, String> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId().toString());
        variables.put("status", event.getStatus());
        variables.put("customerName", event.getCustomerName());
        return variables;
    }

    private void saveNotification(String userId, String templateId, String content,
                                  NotificationType type, String referenceId, String referenceType) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTemplateId(templateId);
        notification.setContent(content);
        notification.setType(type);
        notification.setStatus(NotificationStatus.SENT);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setSentAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }
}