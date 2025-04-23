package com.foodDelivery.paymentService.controller;


import com.foodDelivery.paymentService.dto.PaymentRequest;
import com.foodDelivery.paymentService.dto.PaymentResponse;
import com.foodDelivery.paymentService.dto.PaymentDetails;
import com.foodDelivery.paymentService.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import java.util.List;


@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

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
}