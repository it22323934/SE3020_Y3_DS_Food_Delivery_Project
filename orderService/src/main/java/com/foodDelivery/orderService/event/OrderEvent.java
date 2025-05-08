package com.foodDelivery.orderService.event;

import com.foodDelivery.orderService.dto.ContactInfoRequest;
import com.foodDelivery.orderService.dto.DeliveryAddressRequest;
import com.foodDelivery.orderService.dto.LocationRequest;
import com.foodDelivery.orderService.dto.OrderItemRequest;
import com.foodDelivery.orderService.dto.PromotionDetailsRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String eventId;
    private String eventType;
    private String orderId;
    private Long userId;
    private String restaurantId;
    private List<OrderItemRequest> items;
    private ContactInfoRequest contactInfo;
    private DeliveryAddressRequest deliveryAddress;
    private String deliveryInstructions;
    private String paymentMethod;
    private double subtotal;
    private double taxAmount;
    private double deliveryFee;
    private double discount;
    private double total;
    private LocationRequest deliveryLocation;
    private LocationRequest restaurantLocation;
    private PromotionDetailsRequest promotion;
    private long timestamp;
}