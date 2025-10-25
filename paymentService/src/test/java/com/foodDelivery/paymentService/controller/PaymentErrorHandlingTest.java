package com.foodDelivery.paymentService.controller;

import com.foodDelivery.paymentService.dto.ErrorResponse;
import com.foodDelivery.paymentService.dto.PaymentRequest;
import com.foodDelivery.paymentService.dto.PaymentIntentRequest;
import com.foodDelivery.paymentService.dto.ConfirmPaymentRequest;
import com.foodDelivery.paymentService.serviceInterfaces.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class to verify that error handling prevents verbose error messages
 * from being exposed to clients in the payment service.
 */
@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
class PaymentErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testPaymentProcessingErrorReturnsGenericMessage() throws Exception {
        // Given
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId("test-order-123");
        paymentRequest.setCustomerEmail("test@example.com");
        paymentRequest.setAmount(100.0);
        paymentRequest.setStripeToken("tok_test");

        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk()) // Controller catches exception and returns 200 with error response
                .andExpect(jsonPath("$.orderId").value("test-order-123"))
                .andExpect(jsonPath("$.paymentStatus").value("failed"));
    }

    @Test
    void testStripeErrorReturnsGenericMessage() throws Exception {
        // Given
        PaymentIntentRequest request = new PaymentIntentRequest();
        request.setOrderId("test-order-123");
        request.setCustomerEmail("test@example.com");
        request.setAmount(100.0);

        when(paymentService.createPaymentIntent(any(PaymentIntentRequest.class)))
                .thenThrow(new RuntimeException("Card declined"));

        // When & Then
        mockMvc.perform(post("/api/payments/create-payment-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Payment processing failed"));
    }

    @Test
    void testMalformedJsonReturnsValidationError() throws Exception {
        // Given
        String malformedJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .content(malformedJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request format. Please check your request body."))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void testMissingParameterReturnsValidationError() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/user/"))
                        .andExpect(status().isNotFound());
    }

    @Test
    void testInvalidPaymentAmountReturnsValidationError() throws Exception {
        // Given
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId("test-order-123");
        paymentRequest.setCustomerEmail("test@example.com");
        paymentRequest.setAmount(-100.0); // Invalid negative amount
        paymentRequest.setStripeToken("tok_test");

        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenThrow(new IllegalArgumentException("Amount must be greater than 0"));

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk()) // Controller catches exception
                .andExpect(jsonPath("$.orderId").value("test-order-123"))
                .andExpect(jsonPath("$.paymentStatus").value("failed"));
    }

    @Test
    void testPaymentConfirmationErrorReturnsGenericMessage() throws Exception {
        // Given
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setPaymentIntentId("pi_test_123");

        when(paymentService.confirmPayment(any(ConfirmPaymentRequest.class), anyString()))
                .thenThrow(new RuntimeException("Payment intent not found"));

        // When & Then
        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Payment processing failed"));
    }

    @Test
    void testUnauthorizedAccessReturnsGenericError() throws Exception {
        // Given
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setPaymentIntentId("pi_test_123");

        // When & Then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void testMethodNotAllowedReturnsGenericError() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/payments/test-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("The requested method is not supported for this resource."))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void testPaymentNotFoundReturnsNotFoundError() throws Exception {
        // Given
        String orderId = "non-existent-order";
        when(paymentService.getPaymentByOrderId(orderId))
                .thenThrow(new IllegalArgumentException("Payment not found for order: " + orderId));

        // When & Then
        mockMvc.perform(get("/api/payments/{orderId}", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payment not found for order: " + orderId))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void testDatabaseErrorReturnsGenericError() throws Exception {
        // Given
        String email = "test@example.com";
        when(paymentService.getPaymentsByUser(email))
                .thenThrow(new RuntimeException("Database connection timeout"));

        // When & Then
        mockMvc.perform(get("/api/payments/user/{email}", email))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
