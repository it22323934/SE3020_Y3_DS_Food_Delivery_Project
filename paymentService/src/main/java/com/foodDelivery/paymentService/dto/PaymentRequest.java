package com.foodDelivery.paymentService.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String orderId;
    private String customerEmail;
    private double amount;
    private String stripeToken;

}