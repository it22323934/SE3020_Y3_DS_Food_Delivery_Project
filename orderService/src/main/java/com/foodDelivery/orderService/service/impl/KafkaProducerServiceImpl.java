package com.foodDelivery.orderService.service.impl;

import com.foodDelivery.orderService.dto.OrderCreateRequest;
import com.foodDelivery.orderService.event.OrderEvent;
import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String ORDER_TOPIC = "order-notifications";

    @Override
    public void sendOrderOutForDeliveryEvent(Order order, OrderCreateRequest orderDetails) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_OUT_FOR_DELIVERY")
                    .orderId(order.getId())
                    .userId(orderDetails.getUserId())
                    .restaurantId(orderDetails.getRestaurantId())
                    .items(orderDetails.getItems())
                    .contactInfo(orderDetails.getContactInfo())
                    .deliveryAddress(orderDetails.getDeliveryAddress())
                    .deliveryInstructions(orderDetails.getDeliveryInstructions())
                    .paymentMethod(orderDetails.getPaymentMethod())
                    .subtotal(orderDetails.getSubtotal())
                    .taxAmount(orderDetails.getTaxAmount())
                    .deliveryFee(orderDetails.getDeliveryFee())
                    .discount(orderDetails.getDiscount())
                    .total(orderDetails.getTotal())
                    .deliveryLocation(orderDetails.getDeliveryLocation())
                    .restaurantLocation(orderDetails.getRestaurantLocation())
                    .promotion(orderDetails.getPromotion())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(ORDER_TOPIC, order.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Order out for delivery event sent for order: {}", order.getId());
                        } else {
                            log.error("Failed to send order out for delivery event: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending order out for delivery event: {}", e.getMessage());
        }
    }

    @Override
    public void sendOrderStatusUpdateEvent(Order order) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_STATUS_UPDATED")
                    .orderId(order.getId())
                    .timestamp(System.currentTimeMillis())
                    .build();

            kafkaTemplate.send(ORDER_TOPIC, order.getId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Order status update event sent for order: {}", order.getId());
                        } else {
                            log.error("Failed to send order status update event: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending order status update event: {}", e.getMessage());
        }
    }
}