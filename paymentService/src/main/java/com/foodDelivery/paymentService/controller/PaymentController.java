package com.foodDelivery.paymentService.controller;

import com.foodDelivery.paymentService.config.SecureLogger;
import com.foodDelivery.paymentService.dto.*;
import com.foodDelivery.paymentService.serviceImpl.PaymentServiceImpl;
import com.stripe.exception.StripeException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) {
        SecureLogger.logPaymentOperation("PROCESS_PAYMENT", paymentRequest.getOrderId(), paymentRequest.getCustomerEmail(), Map.of("amount", paymentRequest.getAmount()));
        try {
            PaymentResponse response = paymentService.processPayment(paymentRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            SecureLogger.logError("Payment processing failed for order: {}", e, paymentRequest.getOrderId());
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
    public ResponseEntity<List<PaymentDetails>> getUserPayments(@PathVariable String email, HttpServletRequest request) {
        SecureLogger.logInfo("Fetching payments for user: {}", email);
        List<PaymentDetails> payments = paymentService.getPaymentsByUser(email);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments (Admin-only).
     */
    @GetMapping("/admin/all")
    public ResponseEntity<List<PaymentDetails>> getAllPayments(HttpServletRequest request) {
        SecureLogger.logInfo("Admin fetching all payments");
        List<PaymentDetails> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment details by order ID.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentDetails> getPaymentByOrderId(@PathVariable String orderId, HttpServletRequest request) {
        SecureLogger.logInfo("Fetching payment for order: {}", orderId);
        PaymentDetails payment = paymentService.getPaymentByOrderId(orderId);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody PaymentIntentRequest request, HttpServletRequest httpRequest) {
        SecureLogger.logPaymentOperation("CREATE_PAYMENT_INTENT", request.getOrderId(), request.getCustomerEmail(), Map.of("amount", request.getAmount()));
        try {
            Map<String, Object> response = paymentService.createPaymentIntent(request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            SecureLogger.logError("Stripe error in create payment intent: {}", e, request.getOrderId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment processing failed"));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody ConfirmPaymentRequest request,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader,
                                            HttpServletRequest httpRequest) {
        SecureLogger.logPaymentOperation("CONFIRM_PAYMENT", request.getPaymentIntentId(), "N/A", Map.of("intentId", request.getPaymentIntentId()));
        try {
            // Better token validation
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                SecureLogger.logSecurityEvent("AUTH_FAILURE", "N/A", httpRequest.getRemoteAddr(), "Missing or invalid authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            String token = authHeader.substring(7);
            PaymentResponse response = paymentService.confirmPayment(request, token);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            SecureLogger.logError("Stripe error in confirmation: {}", e, request.getPaymentIntentId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment processing failed"));
        } catch (Exception e) {
            SecureLogger.logError("Payment confirmation failed: {}", e, request.getPaymentIntentId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment processing failed"));
        }
    }

    /**
     * Get failed payments (Admin-only)
     */
    @GetMapping("/admin/failed")
    public ResponseEntity<List<PaymentDetails>> getFailedPayments(HttpServletRequest request) {
        SecureLogger.logInfo("Admin fetching failed payments");
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
