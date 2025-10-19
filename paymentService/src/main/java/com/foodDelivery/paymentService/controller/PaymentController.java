package com.foodDelivery.paymentService.controller;


import com.foodDelivery.paymentService.dto.*;
import com.foodDelivery.paymentService.dto.PaymentDetails;
import com.foodDelivery.paymentService.serviceImpl.PaymentServiceImpl;
import com.stripe.exception.StripeException;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Process payment for an order.
     * SECURITY: Requires authentication - users can only process payments for their own orders
     * Fixed: Added authentication and authorization checks
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestBody PaymentRequest paymentRequest,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate authentication token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Payment processing attempted without valid authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(paymentRequest.getOrderId(), "Authentication required"));
            }

            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String authenticatedUser = authentication.getName();

            logger.info("Processing payment for order {} by user {}",
                paymentRequest.getOrderId(), authenticatedUser);

            // Verify user owns the order before processing payment
            // This will be validated in the service layer
            PaymentResponse response = paymentService.processPayment(paymentRequest, authenticatedUser);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.error("Unauthorized payment attempt for order {}: {}",
                paymentRequest.getOrderId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(paymentRequest.getOrderId(), "Not authorized to process this payment"));
        } catch (Exception e) {
            logger.error("Payment processing failed for order {}: {}",
                paymentRequest.getOrderId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(paymentRequest.getOrderId(), "Payment processing failed"));
        }
    }

    private PaymentResponse createErrorResponse(String orderId, String message) {
        PaymentResponse errorResponse = new PaymentResponse();
        errorResponse.setOrderId(orderId);
        errorResponse.setPaymentStatus("failed");
        return errorResponse;
    }
    /**
     * Get all payments for a specific user (by email).
     * Authorization: Users can only view their own payments, admins can view all
     */
    @GetMapping("/user/{email}")
    @PreAuthorize("@paymentAuthorizationService.canAccessUserPayments(#email, authentication)")
    public ResponseEntity<List<PaymentDetails>> getUserPayments(@PathVariable String email) {
        List<PaymentDetails> payments = paymentService.getPaymentsByUser(email);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get all payments (Admin-only).
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentDetails>> getAllPayments() {
        List<PaymentDetails> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(payments);
    }

    /**
     * Get payment details by order ID.
     * Authorization: Users can only view their own payment, admins can view all
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("@paymentAuthorizationService.canAccessPaymentByOrderId(#orderId, authentication)")
    public ResponseEntity<PaymentDetails> getPaymentByOrderId(@PathVariable String orderId) {
        PaymentDetails payment = paymentService.getPaymentByOrderId(orderId);
        if (payment != null) {
            return ResponseEntity.ok(payment);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Create payment intent for Stripe payment.
     * SECURITY: Requires authentication to prevent anonymous users from creating payment intents
     * Fixed: Added authentication requirement
     */
    @PostMapping("/create-payment-intent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createPaymentIntent(
            @RequestBody PaymentIntentRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String authenticatedUser = authentication.getName();

            logger.info("Creating payment intent for user: {}", authenticatedUser);

            // Pass authenticated user to service for validation
            Map<String, Object> response = paymentService.createPaymentIntent(request, authenticatedUser);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.error("Unauthorized payment intent creation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not authorized to create payment intent"));
        } catch (StripeException e) {
            logger.error("Stripe error creating payment intent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating payment intent: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create payment intent"));
        }
    }

    /**
     * Confirm payment after Stripe processing.
     * SECURITY: Requires authentication and validates user owns the payment
     * Fixed: Made authentication required (not optional)
     */
    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> confirmPayment(
            @RequestBody ConfirmPaymentRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate authentication token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Payment confirmation attempted without valid authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authentication required"));
            }

            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String authenticatedUser = authentication.getName();

            logger.info("Confirming payment by user: {}", authenticatedUser);

            String token = authHeader.substring(7);
            PaymentResponse response = paymentService.confirmPayment(request, token, authenticatedUser);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            logger.error("Unauthorized payment confirmation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Not authorized to confirm this payment"));
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
    @PreAuthorize("hasRole('ADMIN')")
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
