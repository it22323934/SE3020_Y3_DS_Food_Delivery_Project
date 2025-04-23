package com.foodDelivery.paymentService.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentDetails {
    private String orderId;
    private String customerEmail;
    private double amount;
    private String paymentStatus;
    private String stripePaymentId;
    private LocalDateTime paymentDate;
}