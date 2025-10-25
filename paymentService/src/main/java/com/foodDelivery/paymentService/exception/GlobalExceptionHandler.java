package com.foodDelivery.paymentService.exception;

import com.foodDelivery.paymentService.config.SecureLogger;
import com.foodDelivery.paymentService.dto.ErrorResponse;
import com.stripe.exception.StripeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * Global exception handler to provide secure error responses for payment service.
 * This prevents exposure of sensitive technical details to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Handle payment-specific exceptions
     */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(
            PaymentException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Payment error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createPaymentError(
                ex.getMessage(), 
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle Stripe API exceptions
     */
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ErrorResponse> handleStripeException(
            StripeException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logError("Stripe API error - Path: {}", ex, request.getRequestURI());
        
        // Return generic error to client, log details internally
        ErrorResponse errorResponse = ErrorResponse.createPaymentProcessingError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle business validation exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Validation error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                ex.getMessage(), 
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        SecureLogger.logWarn("Validation error: {} - Path: {}", errorMessage, request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                "Validation failed: " + errorMessage,
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        
        String errorMessage = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        
        SecureLogger.logWarn("Constraint violation: {} - Path: {}", errorMessage, request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                "Validation failed: " + errorMessage,
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Authentication error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createUnauthorizedError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Access denied: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("You do not have permission to access this resource.")
                .errorCode("FORBIDDEN")
                .status(403)
                .timestamp(java.time.LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle database access exceptions
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex, 
            HttpServletRequest request) {
        
        // Log the full exception for internal debugging
        SecureLogger.logError("Database access error - Path: {}", ex, request.getRequestURI());
        
        // Return generic error to client
        ErrorResponse errorResponse = ErrorResponse.createGenericError(
                request.getRequestURI(), 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle HTTP message not readable exceptions (malformed JSON)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Malformed request body - Path: {}", request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                "Invalid request format. Please check your request body.",
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle method not supported exceptions
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Method not supported: {} - Path: {}", ex.getMethod(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("The requested method is not supported for this resource.")
                .errorCode("METHOD_NOT_ALLOWED")
                .status(405)
                .timestamp(java.time.LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(
            MissingServletRequestParameterException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Missing parameter: {} - Path: {}", ex.getParameterName(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                "Required parameter '" + ex.getParameterName() + "' is missing.",
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle type mismatch exceptions
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("Type mismatch for parameter: {} - Path: {}", ex.getName(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                "Invalid parameter type for '" + ex.getName() + "'.",
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle no handler found exceptions (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, 
            HttpServletRequest request) {
        
        SecureLogger.logWarn("No handler found: {} - Path: {}", ex.getRequestURL(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createNotFoundError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle all other exceptions (catch-all)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            HttpServletRequest request) {
        
        // Log the full exception for internal debugging
        SecureLogger.logError("Unexpected error occurred - Path: {}", ex, request.getRequestURI());
        
        // Return generic error to client
        ErrorResponse errorResponse = ErrorResponse.createGenericError(
                request.getRequestURI(), 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}