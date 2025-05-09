package com.foodDelivery.paymentService.dto;

import lombok.Data;

@Data
public class ConfirmPaymentRequest {
    private String paymentIntentId;
}