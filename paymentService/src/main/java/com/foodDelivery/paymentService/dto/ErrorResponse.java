package com.foodDelivery.paymentService.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic error response DTO for user-facing error messages.
 * This prevents exposure of sensitive technical details in payment service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private String message;
    private String errorCode;
    private int status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String path;
    
    /**
     * Creates a generic error response for production use
     */
    public static ErrorResponse createGenericError(String path, int status) {
        return ErrorResponse.builder()
                .message("Something went wrong. Please try again later.")
                .errorCode("INTERNAL_ERROR")
                .status(status)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    /**
     * Creates a validation error response
     */
    public static ErrorResponse createValidationError(String message, String path) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode("VALIDATION_ERROR")
                .status(400)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    /**
     * Creates a not found error response
     */
    public static ErrorResponse createNotFoundError(String path) {
        return ErrorResponse.builder()
                .message("The requested resource was not found.")
                .errorCode("NOT_FOUND")
                .status(404)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    /**
     * Creates an unauthorized error response
     */
    public static ErrorResponse createUnauthorizedError(String path) {
        return ErrorResponse.builder()
                .message("You are not authorized to perform this action.")
                .errorCode("UNAUTHORIZED")
                .status(401)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    /**
     * Creates a payment-specific error response
     */
    public static ErrorResponse createPaymentError(String message, String path) {
        return ErrorResponse.builder()
                .message(message)
                .errorCode("PAYMENT_ERROR")
                .status(400)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    
    /**
     * Creates a payment processing error response
     */
    public static ErrorResponse createPaymentProcessingError(String path) {
        return ErrorResponse.builder()
                .message("Payment processing failed. Please try again.")
                .errorCode("PAYMENT_PROCESSING_ERROR")
                .status(500)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}
