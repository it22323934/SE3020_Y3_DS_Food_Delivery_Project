package com.foodDelivery.paymentService.serviceImpl;

import com.foodDelivery.paymentService.dto.PaymentRequest;
import com.foodDelivery.paymentService.dto.PaymentResponse;
import com.foodDelivery.paymentService.dto.PaymentDetails;
import com.foodDelivery.paymentService.model.Payment;
import com.foodDelivery.paymentService.repository.PaymentRepository;
import com.foodDelivery.paymentService.serviceInterfaces.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
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
            // Process payment with Stripe
            Charge charge = Charge.create(createChargeParams(paymentRequest));

            // Save successful payment
            savePaymentRecord(paymentRequest, charge.getId(), charge.getStatus());

            response.setPaymentStatus(charge.getStatus());
            response.setStripePaymentId(charge.getId());

        } catch (StripeException e) {
            // Save failed payment (without error message)
            savePaymentRecord(paymentRequest, null, "failed");

            response.setPaymentStatus("failed");
            // No error message in response
        }
        return response;
    }

    private Map<String, Object> createChargeParams(PaymentRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", (int)(request.getAmount() * 100));
        params.put("currency", "usd");
        params.put("source", request.getStripeToken());
        params.put("description", "Payment for order " + request.getOrderId());
        return params;
    }

    private void savePaymentRecord(PaymentRequest request, String stripeId, String status) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setCustomerEmail(request.getCustomerEmail());
        payment.setAmount(request.getAmount());
        payment.setPaymentStatus(status);
        payment.setStripePaymentId(stripeId); // null for failed payments
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    @Override
    public List<PaymentDetails> getPaymentsByUser(String customerEmail) {
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
        return paymentRepository.findByOrderId(orderId)
                .map(this::mapToPaymentDetails)
                .orElse(null);
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
}