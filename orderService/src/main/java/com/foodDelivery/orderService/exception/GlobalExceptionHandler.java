package com.foodDelivery.orderService.exception;

import com.foodDelivery.orderService.dto.ErrorResponse;
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
 * Global exception handler to provide secure error responses.
 * This prevents exposure of sensitive technical details to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Handle business validation exceptions
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidationException(
            BusinessValidationException ex, 
            HttpServletRequest request) {
        
        log.warn("Business validation error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                ex.getMessage(), 
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle order not found exceptions
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
            OrderNotFoundException ex, 
            HttpServletRequest request) {
        
        log.warn("Order not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createNotFoundError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle restaurant not found exceptions
     */
    @ExceptionHandler(RestaurantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRestaurantNotFoundException(
            RestaurantNotFoundException ex, 
            HttpServletRequest request) {
        
        log.warn("Restaurant not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createNotFoundError(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle illegal state exceptions (business logic violations)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, 
            HttpServletRequest request) {
        
        log.warn("Illegal state error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.createValidationError(
                "Invalid operation. Please check your request and try again.",
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
        
        log.warn("Validation error: {} - Path: {}", errorMessage, request.getRequestURI());
        
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
        
        log.warn("Constraint violation: {} - Path: {}", errorMessage, request.getRequestURI());
        
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
        
        log.warn("Authentication error: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
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
        
        log.warn("Access denied: {} - Path: {}", ex.getMessage(), request.getRequestURI());
        
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
        log.error("Database access error - Path: {}", request.getRequestURI(), ex);
        
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
        
        log.warn("Malformed request body - Path: {}", request.getRequestURI());
        
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
        
        log.warn("Method not supported: {} - Path: {}", ex.getMethod(), request.getRequestURI());
        
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
        
        log.warn("Missing parameter: {} - Path: {}", ex.getParameterName(), request.getRequestURI());
        
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
        
        log.warn("Type mismatch for parameter: {} - Path: {}", ex.getName(), request.getRequestURI());
        
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
        
        log.warn("No handler found: {} - Path: {}", ex.getRequestURL(), request.getRequestURI());
        
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
        log.error("Unexpected error occurred - Path: {}", request.getRequestURI(), ex);
        
        // Return generic error to client
        ErrorResponse errorResponse = ErrorResponse.createGenericError(
                request.getRequestURI(), 
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
