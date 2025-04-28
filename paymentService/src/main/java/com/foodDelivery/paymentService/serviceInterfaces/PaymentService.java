package com.foodDelivery.paymentService.serviceInterfaces;

import com.foodDelivery.paymentService.dto.*;
import com.stripe.exception.StripeException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest paymentRequest);

    List<PaymentDetails> getPaymentsByUser(String customerEmail);

    List<PaymentDetails> getAllPayments();

    PaymentDetails getPaymentByOrderId(String orderId);
    List<PaymentDetails> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<PaymentDetails> getFailedPayments();

    Map<String, Object> createPaymentIntent(PaymentIntentRequest request) throws StripeException;

    //PaymentResponse confirmPayment(ConfirmPaymentRequest request) throws StripeException;

    PaymentResponse confirmPayment(ConfirmPaymentRequest request, String token) throws StripeException;
}