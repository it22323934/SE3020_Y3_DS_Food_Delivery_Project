package com.foodDelivery.notificationService.service;

import com.foodDelivery.notificationService.interfaces.SmsService;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SMSService.class);

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @Override
    public void sendSms(String phoneNumber, String messageBody) {
        try {
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                logger.warn("Cannot send SMS: recipient phone number is empty");
                return;
            }

            Message message = Message.creator(
                            new PhoneNumber(phoneNumber),
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
