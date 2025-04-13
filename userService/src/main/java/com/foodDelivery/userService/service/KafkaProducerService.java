package com.foodDelivery.userService.service;

import com.foodDelivery.userService.event.PasswordResetEvent;
import com.foodDelivery.userService.event.UserRegistrationAdminEvent;
import com.foodDelivery.userService.event.UserRegistrationEvent;
import com.foodDelivery.userService.model.UserNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String REGISTRATION_TOPIC = "user-registration";
    private static final String ADMIN_REGISTRATION_TOPIC = "admin-user-registration";
    private static final String PASSWORD_RESET_TOPIC = "user-password-reset";

    public void sendUserRegistrationEvent(UserRegistrationEvent event) {
        try {
            kafkaTemplate.send(REGISTRATION_TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Message sent successfully to topic: {}, partition: {}, offset: {}",
                                    REGISTRATION_TOPIC, result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send message to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error while sending message to Kafka: {}", e.getMessage());
            throw new RuntimeException("Could not send registration event to Kafka", e);
        }
    }

    public void sendAdminUserRegistrationEvent(UserRegistrationAdminEvent event) {
        try {
            kafkaTemplate.send(ADMIN_REGISTRATION_TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Message sent successfully to ADMIN topic: {}, partition: {}, offset: {}",
                                    ADMIN_REGISTRATION_TOPIC, result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send message to ADMIN Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error while sending message to ADMIN Kafka: {}", e.getMessage());
            throw new RuntimeException("Could not send admin registration event to Kafka", e);
        }
    }

    public void sendPasswordResetEvent(Long userId, String email, String firstName,
                                       String eventType, String resetUrl) {
        try {
            PasswordResetEvent event = new PasswordResetEvent(
                    userId,
                    email,
                    firstName,
                    eventType,
                    resetUrl,
                    System.currentTimeMillis()
            );

            kafkaTemplate.send(PASSWORD_RESET_TOPIC, email, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Password reset event sent successfully to topic: {}, partition: {}, offset: {}",
                                    PASSWORD_RESET_TOPIC, result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send password reset event to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error while sending password reset event to Kafka: {}", e.getMessage());
            throw new RuntimeException("Could not send password reset event to Kafka", e);
        }
    }

    public void sendUserNotification(String email, String eventType, Map<String, Object> data) {
        try {
            UserNotificationEvent event = new UserNotificationEvent();
            event.setEmail(email);
            event.setEventType(eventType);
            event.setData(data);
            event.setTimestamp(System.currentTimeMillis());

            kafkaTemplate.send(PASSWORD_RESET_TOPIC, email, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Notification sent successfully to topic: {}, partition: {}, offset: {}",
                                    PASSWORD_RESET_TOPIC, result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to send notification to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error while sending notification to Kafka: {}", e.getMessage());
            throw new RuntimeException("Could not send notification event to Kafka", e);
        }
    }
}