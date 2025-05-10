package com.foodDelivery.orderService.service.impl;

import com.foodDelivery.orderService.dto.OrderCreateRequest;
import com.foodDelivery.orderService.dto.OrderItemRequest;
import com.foodDelivery.orderService.event.OrderEvent;
import com.foodDelivery.orderService.model.ContactInfo;
import com.foodDelivery.orderService.model.DeliveryAddress;
import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;
import com.foodDelivery.orderService.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements KafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String ORDER_TOPIC = "order-notifications";

    @Override
    public void sendOrderCreatedEvent(Order order, OrderCreateRequest orderDetails) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_CREATED")
                    .orderId(order.getId())
                    .userId(orderDetails.getUserId())
                    .restaurantId(orderDetails.getRestaurantId())
                    .items(orderDetails.getItems())
                    .contactInfo(orderDetails.getContactInfo())
                    .deliveryAddress(orderDetails.getDeliveryAddress())
                    .deliveryInstructions(orderDetails.getDeliveryInstructions())
                    .paymentMethod(orderDetails.getPaymentMethod())
                    .status(OrderStatus.PENDING)
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
                            log.info("Order creation notification sent for order: {}", order.getId());
                        } else {
                            log.error("Failed to send order creation notification: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error sending order creation notification: {}", e.getMessage());
        }
    }

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
    public void sendOrderStatusUpdateEvent(Order order, OrderStatus orderStatus) {
        try {
            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_STATUS_UPDATED")
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .restaurantId(order.getRestaurantId())
                    .items(order.getItems().stream()
                            .map(item -> OrderItemRequest.builder()
                                    .itemId(item.getItemId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .contactInfo(convertContactInfo(order.getContactInfo()))
                    .deliveryAddress(convertDeliveryAddress(order.getDeliveryAddress()))
                    .deliveryInstructions(order.getDeliveryInstructions())
                    .paymentMethod(order.getPaymentMethod())
                    .status(orderStatus)
                    .subtotal(order.getSubtotal())
                    .taxAmount(order.getTaxAmount())
                    .deliveryFee(order.getDeliveryFee())
                    .discount(order.getDiscount())
                    .total(order.getTotal())
                    .deliveryLocation(convertLocation(order.getDeliveryLocation()))
                    .restaurantLocation(convertLocation(order.getRestaurantLocation()))
                    .promotion(convertPromotion(order.getPromotion()))
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

    // Helper methods to convert Order entity fields to DTO objects
    private com.foodDelivery.orderService.dto.ContactInfoRequest convertContactInfo(ContactInfo info) {
        return com.foodDelivery.orderService.dto.ContactInfoRequest.builder()
                .name(info.getName())
                .phone(info.getPhone())
                .email(info.getEmail())
                .build();
    }

    private com.foodDelivery.orderService.dto.DeliveryAddressRequest convertDeliveryAddress(DeliveryAddress address) {
        return com.foodDelivery.orderService.dto.DeliveryAddressRequest.builder()
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .build();
    }

    private com.foodDelivery.orderService.dto.LocationRequest convertLocation(Order.Location location) {
        if (location == null) return null;
        return com.foodDelivery.orderService.dto.LocationRequest.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .address(location.getAddress())
                .name(location.getName())
                .build();
    }

    private com.foodDelivery.orderService.dto.PromotionDetailsRequest convertPromotion(Order.PromotionDetails promotion) {
        if (promotion == null) return null;
        return com.foodDelivery.orderService.dto.PromotionDetailsRequest.builder()
                .code(promotion.getCode())
                .discountAmount(promotion.getDiscountAmount())
                .build();
    }
}