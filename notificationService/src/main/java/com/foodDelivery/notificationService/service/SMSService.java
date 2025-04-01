package com.foodDelivery.notificationService.service;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SMSService {
    private static final Logger logger = LoggerFactory.getLogger(SMSService.class);

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    public void sendSMS(String toPhoneNumber, String messageBody) {
        try {
            if (toPhoneNumber == null || toPhoneNumber.isEmpty()) {
                logger.warn("Cannot send SMS: recipient phone number is empty");
                return;
            }

            Message message = Message.creator(
                            new PhoneNumber(toPhoneNumber),
                            new PhoneNumber(fromPhoneNumber),
                            messageBody)
                    .create();
            logger.info("SMS sent with SID: {}", message.getSid());
        } catch (Exception e) {
            logger.error("Failed to send SMS: {}", e.getMessage(), e);
            throw new RuntimeException("SMS sending failed", e);
        }
    }
}