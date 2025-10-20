package com.foodDelivery.restaurantService.filter;

import com.foodDelivery.restaurantService.util.XssSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Servlet filter that sanitizes all incoming HTTP request parameters and headers
 * to prevent XSS (Cross-Site Scripting) attacks.
 *
 * SECURITY: This filter provides automatic protection against XSS attacks by:
 * 1. Sanitizing all request parameters
 * 2. Sanitizing all request headers (except Authorization)
 * 3. Logging suspicious XSS payloads
 * 4. Wrapping the request to provide sanitized values
 *
 * This filter runs early in the filter chain (Order = 1) to ensure
 * all downstream processing works with sanitized data.
 *
 * @author Security Team
 * @version 1.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class XssProtectionFilter extends OncePerRequestFilter {

    private final XssSanitizer xssSanitizer;

    // Headers that should NOT be sanitized (authentication tokens, etc.)
    private static final List<String> EXCLUDED_HEADERS = Arrays.asList(
            "authorization",
            "content-type",
            "content-length",
            "host",
            "connection",
            "user-agent"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.debug("XSS Protection Filter processing request: {}", request.getRequestURI());

        // Wrap the request to sanitize parameters and headers
        XssProtectionRequestWrapper wrappedRequest = new XssProtectionRequestWrapper(request, xssSanitizer);

        // Check for XSS payloads in parameters (for security logging)
        checkForXssPayloads(request);

        filterChain.doFilter(wrappedRequest, response);
    }

    /**
     * Checks request parameters for XSS payloads and logs them.
     * This helps with security monitoring and incident response.
     */
    private void checkForXssPayloads(HttpServletRequest request) {
        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);

            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (xssSanitizer.containsXssPayload(paramValue)) {
                        log.warn("SECURITY: Potential XSS payload detected in request parameter '{}' from IP: {}",
                                paramName, request.getRemoteAddr());
                        log.warn("SECURITY: Suspicious value: {}", paramValue.substring(0, Math.min(100, paramValue.length())));
                    }
                }
            }
        }
    }

    /**
     * HttpServletRequest wrapper that sanitizes parameter and header values.
     */
    private static class XssProtectionRequestWrapper extends HttpServletRequestWrapper {

        private final XssSanitizer xssSanitizer;

        public XssProtectionRequestWrapper(HttpServletRequest request, XssSanitizer xssSanitizer) {
            super(request);
            this.xssSanitizer = xssSanitizer;
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitizeValue(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }

            String[] sanitizedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = sanitizeValue(values[i]);
            }
            return sanitizedValues;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);

            // Don't sanitize excluded headers
            if (name != null && EXCLUDED_HEADERS.contains(name.toLowerCase())) {
                return value;
            }

            return sanitizeValue(value);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            // Don't sanitize excluded headers
            if (name != null && EXCLUDED_HEADERS.contains(name.toLowerCase())) {
                return super.getHeaders(name);
            }

            Enumeration<String> headers = super.getHeaders(name);
            if (headers == null) {
                return Collections.emptyEnumeration();
            }

            List<String> sanitizedHeaders = Collections.list(headers).stream()
                    .map(this::sanitizeValue)
                    .toList();

            return Collections.enumeration(sanitizedHeaders);
        }

        private String sanitizeValue(String value) {
            if (value == null) {
                return null;
            }
            return xssSanitizer.sanitizeStrict(value);
        }
    }
}
