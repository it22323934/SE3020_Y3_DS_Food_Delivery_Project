package com.foodDelivery.paymentService.serviceInterfaces;

import com.foodDelivery.paymentService.dto.PaymentDetails;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentReportService {
    byte[] generatePaymentReportByOrder(String orderId);
    byte[] generatePaymentReportByUser(String email);
    byte[] generatePaymentReportByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    byte[] generatePaymentReportByStatus(String status);
    List<String> getAvailableReportTypes();
}