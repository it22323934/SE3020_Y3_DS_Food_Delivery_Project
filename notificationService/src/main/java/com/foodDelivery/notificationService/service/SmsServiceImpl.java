package com.foodDelivery.notificationService.service;

import com.foodDelivery.notificationService.interfaces.SmsService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @Override
    public void sendSms(String phoneNumber, String messageBody) {
        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                log.warn("Cannot send SMS: recipient phone number is empty");
                return;
            }

            // Format phone number - ensure it starts with '+' for international format
            String formattedPhoneNumber = formatPhoneNumber(phoneNumber);

            Message message = Message.creator(
                            new PhoneNumber(formattedPhoneNumber),
                            new PhoneNumber(fromPhoneNumber),
                            messageBody)
                    .create();
            log.info("SMS sent with SID: {}", message.getSid());
        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage(), e);
            throw new RuntimeException("SMS sending failed", e);
        }
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Check if phone number already starts with '+', if not, add it
        if (!phoneNumber.startsWith("+")) {
            return "+" + phoneNumber;
        }
        return phoneNumber;
    }
}