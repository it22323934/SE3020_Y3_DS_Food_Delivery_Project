package com.foodDelivery.paymentService.controller;


import com.foodDelivery.paymentService.dto.*;
import com.foodDelivery.paymentService.dto.PaymentDetails;
import com.foodDelivery.paymentService.serviceImpl.PaymentServiceImpl;
import com.stripe.exception.StripeException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;



@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentServiceImpl paymentService;

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest paymentRequest) {
        try {
            PaymentResponse response = paymentService.processPayment(paymentRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setOrderId(paymentRequest.getOrderId());
            errorResponse.setPaymentStatus("failed");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get all payments for a specific user (by email).
     */
    @GetMapping("/user/{email}")
    public ResponseEntity<List<PaymentDetails>> getUserPayments(@PathVariable String email) {
        List<PaymentDetails> payments = paymentService.getPaymentsByUser(email);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments (Admin-only).
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<PaymentDetails>> getAllPayments() {
        List<PaymentDetails> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment details by order ID.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentDetails> getPaymentByOrderId(@PathVariable String orderId) {
        PaymentDetails payment = paymentService.getPaymentByOrderId(orderId);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        try {
            Map<String, Object> response = paymentService.createPaymentIntent(request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody ConfirmPaymentRequest request,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Better token validation
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            String token = authHeader.substring(7);
            PaymentResponse response = paymentService.confirmPayment(request, token);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error in confirmation: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Stripe error",
                            "message", e.getMessage(),
                            "stripeError", e.getStripeError() != null ? e.getStripeError().getCode() : null
                    ));
        } catch (Exception e) {
            logger.error("Payment confirmation failed: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Payment processing failed",
                            "message", e.getMessage(),
                            "cause", e.getCause() != null ? e.getCause().getMessage() : null
                    ));
        }
    }

    /**
     * Get failed payments (Admin-only)
     */
    @GetMapping("/admin/failed")
    public ResponseEntity<List<PaymentDetails>> getFailedPayments() {
        List<PaymentDetails> payments = paymentService.getFailedPayments();
        return ResponseEntity.ok(payments);
    }

//    /**
//     * Get payments by date range (Admin-only)
//     */
//    @GetMapping("/admin/range")
//    public ResponseEntity<List<PaymentDetails>> getPaymentsByDateRange(
//            @RequestParam String startDate,
//            @RequestParam String endDate) {
//        List<PaymentDetails> payments = paymentService.getPaymentsByDateRange(startDate, endDate);
//        return ResponseEntity.ok(payments);
//    }
//
}
