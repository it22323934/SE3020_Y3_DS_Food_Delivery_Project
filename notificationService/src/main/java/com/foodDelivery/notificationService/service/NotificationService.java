// notificationService/src/main/java/com/foodDelivery/notificationService/service/NotificationService.java
package com.foodDelivery.notificationService.service;

import com.foodDelivery.notificationService.event.OrderStatusEvent;
import com.foodDelivery.userService.event.PasswordResetEvent;
import com.foodDelivery.userService.event.UserRegistrationEvent;
import com.foodDelivery.notificationService.model.*;
import com.foodDelivery.notificationService.repository.NotificationRepository;
import com.foodDelivery.notificationService.repository.NotificationTemplateRepository;
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
import java.util.Optional;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender javaMailSender;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationRepository notificationRepository;
    private final SMSService smsService;
    private final PushNotificationService pushService;

    public NotificationService(JavaMailSender javaMailSender,
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

        // Send email notification
        sendEmailNotification(event);

        // Send SMS notification if phone number exists
        if (event.getPhoneNumber() != null && !event.getPhoneNumber().isEmpty()) {
            sendSmsNotification(event);
        }
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
            // Use default template if none found
            MimeMessagePreparator messagePreparatory = mimeMessage -> {
                MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
                messageHelper.setFrom("asirijayawardena920@gmail.com");
                messageHelper.setTo(event.getEmail());
                messageHelper.setSubject("Welcome to Food Delivery - Confirm Your Registration");
                messageHelper.setText(String.format("""
                    Dear %s %s,
                    
                    Welcome to our Food Delivery Service! Please confirm your registration by clicking the link below:
                    
                    %s
                    
                    This link will expire in 24 hours.
                    
                    Best Regards,
                    Food Delivery Team
                    """,
                        event.getFirstName(),
                        event.getLastName(),
                        event.getConfirmationUrl()), true);
            };

            try {
                javaMailSender.send(messagePreparatory);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                logger.info("Registration confirmation email sent to {}", event.getEmail());
            } catch (MailException e) {
                notification.setStatus(NotificationStatus.FAILED);
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