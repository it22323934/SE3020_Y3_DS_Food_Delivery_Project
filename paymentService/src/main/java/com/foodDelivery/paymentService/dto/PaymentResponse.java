package com.foodDelivery.paymentService.dto;


import lombok.Data;

@Data
public class PaymentResponse {
    private String orderId;
    private String paymentStatus;
    private String stripePaymentId;
    private String errorMessage;
}