package com.foodDelivery.paymentService.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String customerEmail;
    private double amount;
    private String paymentStatus;
    private String stripePaymentId;
    private LocalDateTime paymentDate;
}