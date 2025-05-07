package Delivery_Replication.Delivery_Replication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {
    
    @KafkaListener(topics = "order-notifications", groupId = "delivery-service")
    public void handleOrderEvent(com.foodDelivery.orderService.event.OrderEvent event) {
        log.info("Received order event: {}, type: {}", event.getOrderId(), event.getEventType());
        
        if ("ORDER_OUT_FOR_DELIVERY".equals(event.getEventType())) {
            log.info("Processing out for delivery order: {}", event.getOrderId());
            log.info("Order details: {}", event);
        }
    }
}