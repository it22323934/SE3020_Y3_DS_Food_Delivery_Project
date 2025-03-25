package com.foodDelivery.notificationService.service;

import com.foodDelivery.userService.event.UserRegistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender javaMailSender;

    public NotificationService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @KafkaListener(topics = "user-registration", groupId = "notification-service")
    public void handleUserRegistration(UserRegistrationEvent event) {
        logger.info("Received user registration event: {}", event);

        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("springshop@email.com");
            messageHelper.setTo(event.getEmail());
            messageHelper.setSubject("Welcome to Spring Shop - Confirm Your Registration");
            messageHelper.setText(String.format("""
                Dear %s %s,
                
                Welcome to Spring Shop! Please confirm your registration by clicking the link below:
                
                %s
                
                This link will expire in 24 hours.
                
                Best Regards,
                Spring Shop Team
                """,
                    event.getFirstName(),
                    event.getLastName(),
                    event.getConfirmationUrl()), true);
        };

        try {
            javaMailSender.send(messagePreparator);
            logger.info("Registration confirmation email sent to {}", event.getEmail());
        } catch (MailException e) {
            logger.error("Failed to send registration email", e);
            throw new RuntimeException("Failed to send registration email", e);
        }
    }
}