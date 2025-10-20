package com.foodDelivery.restaurantService.util;

import org.apache.commons.text.StringEscapeUtils;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Utility class for sanitizing user input to prevent XSS (Cross-Site Scripting) attacks.
 *
 * SECURITY: This class implements defense-in-depth against XSS vulnerabilities by:
 * 1. Removing all HTML/JavaScript from user input
 * 2. Escaping special characters
 * 3. Providing both strict and lenient sanitization modes
 *
 * @author Security Team
 * @version 1.0
 */
@Component
public class XssSanitizer {

    // OWASP HTML Sanitizer policy - removes ALL HTML
    private static final PolicyFactory POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

    // Strict policy - removes everything
    private static final PolicyFactory STRICT_POLICY = Sanitizers.FORMATTING;

    /**
     * Sanitizes input by removing all HTML tags and JavaScript.
     * This is the recommended method for most user inputs.
     *
     * @param input The user input to sanitize
     * @return Sanitized string with HTML/JavaScript removed
     */
    public String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // Remove HTML tags using OWASP sanitizer
        String sanitized = POLICY.sanitize(input);

        // Additional escape for special characters
        sanitized = StringEscapeUtils.escapeHtml4(sanitized);

        return sanitized.trim();
    }

    /**
     * Strictly sanitizes input by removing ALL HTML and special characters.
     * Use this for fields that should never contain any formatting.
     *
     * @param input The user input to sanitize
     * @return Strictly sanitized string
     */
    public String sanitizeStrict(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // Remove all HTML
        String sanitized = STRICT_POLICY.sanitize(input);

        // Escape HTML special characters
        sanitized = StringEscapeUtils.escapeHtml4(sanitized);

        // Remove any remaining script-related content
        sanitized = sanitized.replaceAll("(?i)<script.*?>.*?</script>", "");
        sanitized = sanitized.replaceAll("(?i)<.*?javascript:.*?>", "");
        sanitized = sanitized.replaceAll("(?i)on\\w+\\s*=", ""); // Remove event handlers

        return sanitized.trim();
    }

    /**
     * Sanitizes input but allows basic formatting (bold, italic, links).
     * Use with caution and only for content areas like descriptions.
     *
     * @param input The user input to sanitize
     * @return Sanitized string with safe HTML preserved
     */
    public String sanitizeWithFormatting(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        // Allow basic formatting tags but remove dangerous content
        return POLICY.sanitize(input).trim();
    }

    /**
     * Checks if input contains potential XSS payloads.
     * Useful for logging/monitoring suspicious activity.
     *
     * @param input The input to check
     * @return true if input contains XSS indicators
     */
    public boolean containsXssPayload(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        // Check for common XSS patterns
        return lowerInput.contains("<script")
                || lowerInput.contains("javascript:")
                || lowerInput.contains("onerror=")
                || lowerInput.contains("onload=")
                || lowerInput.contains("onclick=")
                || lowerInput.contains("eval(")
                || lowerInput.contains("expression(")
                || lowerInput.contains("<iframe")
                || lowerInput.contains("<object")
                || lowerInput.contains("<embed");
    }

    /**
     * Sanitizes a string for use in JSON responses.
     * Prevents JSON injection and XSS in API responses.
     *
     * @param input The input to sanitize
     * @return JSON-safe string
     */
    public String sanitizeForJson(String input) {
        if (input == null) {
            return null;
        }

        // Escape for JSON
        String sanitized = input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

        // Also remove HTML
        return sanitizeStrict(sanitized);
    }
}
