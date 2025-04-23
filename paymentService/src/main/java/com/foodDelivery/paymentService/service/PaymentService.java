package com.foodDelivery.paymentService.service;


import com.foodDelivery.paymentService.dto.PaymentRequest;
import com.foodDelivery.paymentService.dto.PaymentResponse;
import com.foodDelivery.paymentService.model.Payment;
import com.foodDelivery.paymentService.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.foodDelivery.paymentService.dto.PaymentDetails;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentResponse processPayment(PaymentRequest paymentRequest) throws StripeException {

        System.out.println("Received payment request: " + paymentRequest.toString());

        // Create charge with Stripe
        Map<String, Object> chargeParams = new HashMap<>();
        chargeParams.put("amount", (int)(paymentRequest.getAmount() * 100)); // in cents
        chargeParams.put("currency", "usd");
        chargeParams.put("source", paymentRequest.getStripeToken());
        chargeParams.put("description", "Payment for order " + paymentRequest.getOrderId());

        Charge charge = Charge.create(chargeParams);

        // Save payment details to database
        Payment payment = new Payment();
        payment.setOrderId(paymentRequest.getOrderId());
        payment.setCustomerEmail(paymentRequest.getCustomerEmail());
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentStatus(charge.getStatus());
        payment.setStripePaymentId(charge.getId());
        payment.setPaymentDate(LocalDateTime.now());

        paymentRepository.save(payment);

        // Return response
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(paymentRequest.getOrderId());
        response.setPaymentStatus(charge.getStatus());
        response.setStripePaymentId(charge.getId());

        return response;
    }


    /**
     * Get all payments made by a specific user.
     */
    public List<PaymentDetails> getPaymentsByUser(String customerEmail) {
        return paymentRepository.findByCustomerEmail(customerEmail)
                .stream()
                .map(this::mapToPaymentDetails)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments (for admin).
     */
    public List<PaymentDetails> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc()
                .stream()
                .map(this::mapToPaymentDetails)
                .collect(Collectors.toList());
    }

    /**
     * Get payment details by order ID.
     */
    public PaymentDetails getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::mapToPaymentDetails)
                .orElse(null);
    }

    /**
     * Helper method to convert Payment entity to PaymentDetails DTO.
     */
    private PaymentDetails mapToPaymentDetails(Payment payment) {
        PaymentDetails details = new PaymentDetails();
        details.setOrderId(payment.getOrderId());
        details.setCustomerEmail(payment.getCustomerEmail());
        details.setAmount(payment.getAmount());
        details.setPaymentStatus(payment.getPaymentStatus());
        details.setStripePaymentId(payment.getStripePaymentId());
        details.setPaymentDate(payment.getPaymentDate());
        return details;
    }
}