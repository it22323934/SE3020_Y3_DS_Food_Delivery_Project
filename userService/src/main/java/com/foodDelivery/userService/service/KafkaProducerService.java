package com.foodDelivery.userService.service;

import com.foodDelivery.userService.event.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user-registration";

    public void sendUserRegistrationEvent(UserRegistrationEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Message sent successfully to topic: {}, partition: {}, offset: {}",
                                    TOPIC, result.getRecordMetadata().partition(),
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
}