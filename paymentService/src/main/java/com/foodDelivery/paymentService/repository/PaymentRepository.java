package com.foodDelivery.paymentService.repository;



import com.foodDelivery.paymentService.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Find all payments by a user (email)
    List<Payment> findByCustomerEmail(String customerEmail);

    // Find all payments (sorted by latest first)
    List<Payment> findAllByOrderByPaymentDateDesc();

    // Find a payment by order ID
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByPaymentStatus(String status);
    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Payment> findByPaymentDateBetweenOrderByPaymentDateDesc(LocalDateTime startDate, LocalDateTime endDate);

    List<Payment> findByPaymentStatusIgnoreCaseOrderByPaymentDateDesc(String status);

    List<Payment> findByCustomerEmailOrderByPaymentDateDesc(String email);
}