package com.foodDelivery.orderService.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private Long userId;
    private String restaurantId;
    private List<OrderItem> items;
    private ContactInfo contactInfo;
    private DeliveryAddress deliveryAddress;
    private String deliveryInstructions;
    private String paymentMethod;
    private OrderStatus status;
    private double subtotal;
    private double taxAmount;
    private double deliveryFee;
    private double discount;
    private double total;
    private String promotionCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}