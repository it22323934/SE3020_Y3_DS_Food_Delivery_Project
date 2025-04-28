package com.foodDelivery.paymentService.serviceImpl;

import com.foodDelivery.paymentService.dto.*;
import com.foodDelivery.paymentService.dto.PaymentResponse;
import com.foodDelivery.paymentService.dto.PaymentDetails;
import com.foodDelivery.paymentService.model.Payment;
import com.foodDelivery.paymentService.repository.PaymentRepository;
import com.foodDelivery.paymentService.serviceInterfaces.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Autowired
    private PaymentRepository paymentRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(paymentRequest.getOrderId());

        try {
            // Validate payment request first
            validatePaymentRequest(paymentRequest);

            // Process payment with Stripe
            Charge charge = Charge.create(createChargeParams(paymentRequest));

            // Save successful payment
            savePaymentRecord(paymentRequest, charge.getId(), charge.getStatus());

            response.setPaymentStatus(charge.getStatus());
            response.setStripePaymentId(charge.getId());

        } catch (StripeException e) {
            // Save failed payment with error details
            savePaymentRecord(paymentRequest, null, "failed");

            response.setPaymentStatus("failed");
            response.setErrorMessage(e.getUserMessage() != null ?
                    e.getUserMessage() : "Payment processing failed");

        } catch (IllegalArgumentException e) {
            response.setPaymentStatus("failed");
            response.setErrorMessage(e.getMessage());
        }

        return response;
    }

    private void validatePaymentRequest(PaymentRequest request) {
        if (request.getOrderId() == null || request.getOrderId().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }

        if (request.getCustomerEmail() == null || request.getCustomerEmail().isEmpty()) {
            throw new IllegalArgumentException("Customer email is required");
        }

        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (request.getStripeToken() == null || request.getStripeToken().isEmpty()) {
            throw new IllegalArgumentException("Stripe token is required");
        }
    }

    private Map<String, Object> createChargeParams(PaymentRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", (int)(request.getAmount() * 100)); // Convert to cents
        params.put("currency", "usd");
        params.put("source", request.getStripeToken());
        params.put("description", "Payment for order " + request.getOrderId());
        params.put("receipt_email", request.getCustomerEmail()); // Send receipt to customer
        return params;
    }

    private void savePaymentRecord(PaymentRequest request, String stripeId, String status) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setAmount(request.getAmount());
        payment.setPaymentStatus(status);
        payment.setStripePaymentId(stripeId);
        payment.setPaymentDate(LocalDateTime.now());

        // Add additional metadata for tracking
//        payment.setMetadata(Map.of(
//                "payment_attempt", LocalDateTime.now().toString(),
//                "source", "web_checkout"
//        ));

        paymentRepository.save(payment);
    }

    @Override
    public List<PaymentDetails> getPaymentsByUser(String customerEmail) {
        if (customerEmail == null || customerEmail.isEmpty()) {
            throw new IllegalArgumentException("Customer email is required");
        }

        return paymentRepository.findByCustomerEmail(customerEmail)
                .stream()
                .map(this::mapToPaymentDetails)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDetails> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc()
                .stream()
                .map(this::mapToPaymentDetails)
                .collect(Collectors.toList());
    }

    @Override
    public PaymentDetails getPaymentByOrderId(String orderId) {
        if (orderId == null || orderId.isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }

        return paymentRepository.findByOrderId(orderId)
                .map(this::mapToPaymentDetails)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order: " + orderId));
    }

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

    @Override
    public List<PaymentDetails> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Both start and end dates are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        return paymentRepository.findByPaymentDateBetween(startDate, endDate)
                .stream()
                .map(this::mapToPaymentDetails)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDetails> getFailedPayments() {
        return paymentRepository.findByPaymentStatus("failed")
                .stream()
                .map(this::mapToPaymentDetails)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createPaymentIntent(PaymentIntentRequest request) throws StripeException {
        System.out.println("Received amount: " + request.getAmount()); // Verify backend amount



        // Debug logging
        System.out.println("Creating payment intent for amount: $" + request.getAmount());

        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        double roundedAmount = Math.round(request.getAmount() * 100) / 100.0;

        Map<String, Object> params = new HashMap<>();
        params.put("amount", (int)Math.round(request.getAmount() * 100)); // Proper rounding
        params.put("currency", "usd"); // Convert to cents
        System.out.println("Converted to cents: " + params.get("amount")); // Debug
        params.put("currency", "usd");
        params.put("payment_method_types", List.of("card"));


        // Add metadata for later reference
        Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", request.getOrderId());
        metadata.put("customer_email", request.getCustomerEmail());
        params.put("metadata", metadata);

        // For automatic capture (set to false if you want manual confirmation)
        params.put("capture_method", "automatic");

        PaymentIntent intent = PaymentIntent.create(params);

        Map<String, Object> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());
        response.put("paymentIntentId", intent.getId());
        return response;
    }

    @Override
    public PaymentResponse confirmPayment(ConfirmPaymentRequest request, String token) throws StripeException {
        try {
            // Initialize Stripe with API key
            Stripe.apiKey = stripeSecretKey;

            // 1. Retrieve payment intent
            PaymentIntent intent = PaymentIntent.retrieve(request.getPaymentIntentId());
            logger.info("Retrieved payment intent: {}", intent);

            // 2. Validate metadata exists
            if (intent.getMetadata() == null || intent.getMetadata().isEmpty()) {
                throw new IllegalArgumentException("Payment intent missing required metadata");
            }

            String orderId = intent.getMetadata().get("order_id");
            String customerEmail = intent.getMetadata().get("customer_email");

            if (orderId == null || customerEmail == null) {
                throw new IllegalArgumentException("Missing order_id or customer_email in metadata");
            }

            // 3. Create and save payment record
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setCustomerEmail(customerEmail);
            payment.setAmount(intent.getAmount() / 100.0); // Convert from cents
            payment.setPaymentStatus(intent.getStatus());
            payment.setStripePaymentId(intent.getId());
            payment.setPaymentDate(LocalDateTime.now());

            // Save to database
            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Saved payment record with ID: {}", savedPayment.getId());

            // 4. Return response
            PaymentResponse response = new PaymentResponse();
            response.setOrderId(orderId);
            response.setPaymentStatus("succeeded");
            response.setStripePaymentId(intent.getId());
            response.setCustomerEmail(customerEmail);
            response.setAmount(payment.getAmount());

            return response;
        } catch (Exception e) {
            logger.error("Error in confirmPayment: ", e);
            throw new RuntimeException("Payment processing error", e);
        }
    }}