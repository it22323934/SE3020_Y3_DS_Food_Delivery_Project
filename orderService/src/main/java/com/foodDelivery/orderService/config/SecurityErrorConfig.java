package com.foodDelivery.orderService.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Security configuration for error handling.
 * This ensures that sensitive information is not exposed in error responses.
 */
@Configuration
public class SecurityErrorConfig {

    /**
     * Custom error attributes that sanitize error responses in production
     */
    @Bean
    @Profile("prod")
    public DefaultErrorAttributes secureErrorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
                
                // Remove sensitive information in production
                errorAttributes.remove("exception");
                errorAttributes.remove("trace");
                errorAttributes.remove("errors");
                errorAttributes.remove("message");
                
                // Add generic error message
                errorAttributes.put("message", "An error occurred while processing your request");
                errorAttributes.put("error", "Internal Server Error");
                
                return errorAttributes;
            }
        };
    }

    /**
     * Development error attributes that include more details for debugging
     */
    @Bean
    @Profile("dev")
    public DefaultErrorAttributes developmentErrorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
                
                // In development, we can include more details for debugging
                // but still sanitize sensitive information
                if (errorAttributes.containsKey("message")) {
                    String message = (String) errorAttributes.get("message");
                    errorAttributes.put("message", sanitizeErrorMessage(message));
                }
                
                return errorAttributes;
            }
        };
    }

    /**
     * Sanitizes error messages to remove sensitive information
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) return null;
        
        // Remove common sensitive patterns
        String sanitized = message
                .replaceAll("(?i)password[\\s=:]+[^\\s,}]+", "password=***MASKED***")
                .replaceAll("(?i)token[\\s=:]+[^\\s,}]+", "token=***MASKED***")
                .replaceAll("(?i)secret[\\s=:]+[^\\s,}]+", "secret=***MASKED***")
                .replaceAll("(?i)key[\\s=:]+[^\\s,}]+", "key=***MASKED***")
                .replaceAll("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", "***@***.***")
                .replaceAll("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b", "***-***-****");
        
        return sanitized;
    }
}
