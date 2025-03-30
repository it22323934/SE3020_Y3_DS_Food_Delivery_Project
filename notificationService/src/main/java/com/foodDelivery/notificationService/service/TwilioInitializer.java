package com.foodDelivery.notificationService.service;

import com.twilio.Twilio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioInitializer {

    private static final Logger logger = LoggerFactory.getLogger(TwilioInitializer.class);

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @Autowired
    public TwilioInitializer(){
            Twilio.init("AC47d0f30d5a7597657f50a20654825987", "56b178d120f6bad38b60632db0b991f7");
            logger.info("Twilio initialized with Account SID: {}", "AC47d0f30d5a7597657f50a20654825987");
    }

}
