package com.foodDelivery.paymentService.serviceInterfaces;

import com.foodDelivery.paymentService.dto.PaymentRequest;
import com.foodDelivery.paymentService.dto.PaymentResponse;
import com.foodDelivery.paymentService.dto.PaymentDetails;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest paymentRequest);

    List<PaymentDetails> getPaymentsByUser(String customerEmail);

    List<PaymentDetails> getAllPayments();

    PaymentDetails getPaymentByOrderId(String orderId);
    List<PaymentDetails> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<PaymentDetails> getFailedPayments();
}