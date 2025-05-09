package com.foodDelivery.paymentService.dto;

import lombok.Data;

@Data
public class PaymentIntentRequest {
    private String orderId;
    private String customerEmail;
    private double amount;
}