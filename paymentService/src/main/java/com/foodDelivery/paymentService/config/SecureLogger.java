package com.foodDelivery.paymentService.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Secure logging utility to prevent exposure of sensitive information in logs.
 * This class provides methods to safely log information without exposing
 * sensitive data like passwords, tokens, payment details, or personal information.
 */
@Slf4j
@Component
public class SecureLogger {

    // Patterns for sensitive data that should be masked
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password|pass|pwd)\\s*[:=]\\s*[^\\s,}]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?i)(token|auth|jwt|bearer|stripe)\\s*[:=]\\s*[^\\s,}]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern STRIPE_KEY_PATTERN = Pattern.compile("(sk_|pk_)[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAYMENT_ID_PATTERN = Pattern.compile("(pi_|ch_|pm_)[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    private static final String MASKED_VALUE = "***MASKED***";

    /**
     * Logs a secure info message, masking any sensitive information
     */
    public static void logInfo(String message, Object... args) {
        String sanitizedMessage = sanitizeMessage(message);
        log.info(sanitizedMessage, sanitizeArgs(args));
    }

    /**
     * Logs a secure warning message, masking any sensitive information
     */
    public static void logWarn(String message, Object... args) {
        String sanitizedMessage = sanitizeMessage(message);
        log.warn(sanitizedMessage, sanitizeArgs(args));
    }

    /**
     * Logs a secure error message, masking any sensitive information
     */
    public static void logError(String message, Throwable throwable, Object... args) {
        String sanitizedMessage = sanitizeMessage(message);
        Object[] sanitizedArgs = sanitizeArgs(args);
        log.error(sanitizedMessage, sanitizedArgs);
        
        // Log the full exception details for internal debugging
        if (throwable != null) {
            log.error("Full exception details for internal debugging: ", throwable);
        }
    }

    /**
     * Logs security-related events with additional context
     */
    public static void logSecurityEvent(String event, String userId, String ipAddress, String details) {
        log.warn("SECURITY_EVENT: {} | User: {} | IP: {} | Details: {}", 
                event, userId, ipAddress, sanitizeMessage(details));
    }

    /**
     * Logs authentication events securely
     */
    public static void logAuthenticationEvent(String event, String userId, boolean success, String details) {
        String status = success ? "SUCCESS" : "FAILURE";
        log.info("AUTH_EVENT: {} | User: {} | Status: {} | Details: {}", 
                event, userId, status, sanitizeMessage(details));
    }

    /**
     * Logs payment operations with sanitized data
     */
    public static void logPaymentOperation(String operation, String orderId, String customerEmail, Map<String, Object> data) {
        Map<String, Object> sanitizedData = sanitizeMap(data);
        log.info("PAYMENT_OPERATION: {} | Order: {} | Customer: {} | Data: {}", 
                operation, orderId, sanitizeEmail(customerEmail), sanitizedData);
    }

    /**
     * Logs payment processing events securely
     */
    public static void logPaymentProcessing(String operation, String orderId, String status, String details) {
        log.info("PAYMENT_PROCESSING: {} | Order: {} | Status: {} | Details: {}", 
                operation, orderId, status, sanitizeMessage(details));
    }

    /**
     * Logs Stripe API calls securely
     */
    public static void logStripeOperation(String operation, String endpoint, String method, int statusCode) {
        log.info("STRIPE_OPERATION: {} | Endpoint: {} | Method: {} | Status: {}", 
                operation, endpoint, method, statusCode);
    }

    /**
     * Logs database operations securely
     */
    public static void logDatabaseOperation(String operation, String table, String orderId) {
        log.info("DB_OPERATION: {} | Table: {} | Order: {}", operation, table, orderId);
    }

    /**
     * Sanitizes a message by masking sensitive information
     */
    private static String sanitizeMessage(String message) {
        if (message == null) return null;
        
        String sanitized = message;
        
        // Mask passwords
        sanitized = PASSWORD_PATTERN.matcher(sanitized).replaceAll("$1=***MASKED***");
        
        // Mask tokens and API keys
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("$1=***MASKED***");
        
        // Mask Stripe keys
        sanitized = STRIPE_KEY_PATTERN.matcher(sanitized).replaceAll("***MASKED***");
        
        // Mask payment IDs
        sanitized = PAYMENT_ID_PATTERN.matcher(sanitized).replaceAll("***MASKED***");
        
        // Mask email addresses (keep domain for debugging)
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("***@$1");
        
        // Mask phone numbers
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("***-***-****");
        
        // Mask credit card numbers
        sanitized = CREDIT_CARD_PATTERN.matcher(sanitized).replaceAll("****-****-****-****");
        
        return sanitized;
    }

    /**
     * Sanitizes email addresses specifically
     */
    private static String sanitizeEmail(String email) {
        if (email == null) return null;
        return EMAIL_PATTERN.matcher(email).replaceAll("***@$1");
    }

    /**
     * Sanitizes arguments by masking sensitive information
     */
    private static Object[] sanitizeArgs(Object... args) {
        if (args == null) return null;
        
        Object[] sanitizedArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                sanitizedArgs[i] = sanitizeMessage((String) args[i]);
            } else if (args[i] instanceof Map) {
                sanitizedArgs[i] = sanitizeMap((Map<String, Object>) args[i]);
            } else {
                sanitizedArgs[i] = args[i];
            }
        }
        return sanitizedArgs;
    }

    /**
     * Sanitizes a map by masking sensitive values
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> sanitizeMap(Map<String, Object> data) {
        if (data == null) return null;
        
        Map<String, Object> sanitized = new java.util.HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Check if the key indicates sensitive data
            if (isSensitiveKey(key)) {
                sanitized.put(key, MASKED_VALUE);
            } else if (value instanceof String) {
                sanitized.put(key, sanitizeMessage((String) value));
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                sanitized.put(key, sanitizeMap(mapValue));
            } else {
                sanitized.put(key, value);
            }
        }
        return sanitized;
    }

    /**
     * Checks if a key indicates sensitive data
     */
    private static boolean isSensitiveKey(String key) {
        if (key == null) return false;
        
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") ||
               lowerKey.contains("token") ||
               lowerKey.contains("secret") ||
               lowerKey.contains("key") ||
               lowerKey.contains("auth") ||
               lowerKey.contains("credential") ||
               lowerKey.contains("stripe") ||
               lowerKey.contains("payment") ||
               lowerKey.contains("card") ||
               lowerKey.contains("ssn") ||
               lowerKey.contains("social") ||
               lowerKey.contains("credit");
    }
}
