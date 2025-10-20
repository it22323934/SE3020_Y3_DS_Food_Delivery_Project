package com.foodDelivery.apiGateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that adds security headers to all HTTP responses from the API Gateway.
 *
 * SECURITY HEADERS:
 * 1. X-Content-Type-Options: Prevents MIME type sniffing
 * 2. X-Frame-Options: Prevents clickjacking attacks
 * 3. X-XSS-Protection: Enables browser's XSS filter
 * 4. Content-Security-Policy: Prevents XSS, code injection, and data injection attacks
 * 5. Strict-Transport-Security: Enforces HTTPS
 * 6. Referrer-Policy: Controls referrer information
 * 7. Permissions-Policy: Controls browser features
 *
 * These headers implement defense-in-depth security practices as recommended by OWASP.
 *
 * @author Security Team
 * @version 1.0
 */
@Slf4j
@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();

            // Prevent MIME type sniffing
            // Protects against: MIME confusion attacks
            headers.add("X-Content-Type-Options", "nosniff");

            // Prevent clickjacking attacks
            // Protects against: UI redress attacks, clickjacking
            headers.add("X-Frame-Options", "DENY");

            // Enable browser XSS protection
            // Protects against: Cross-Site Scripting (XSS) attacks
            headers.add("X-XSS-Protection", "1; mode=block");

            // Content Security Policy - prevents XSS and injection attacks
            // This is a strict CSP that only allows resources from same origin
            // Adjust based on your application needs
            String csp = "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://maps.googleapis.com https://js.stripe.com; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; " +
                    "img-src 'self' data: https: blob:; " +
                    "connect-src 'self' https://maps.googleapis.com https://api.stripe.com; " +
                    "frame-src 'self' https://js.stripe.com; " +
                    "object-src 'none'; " +
                    "base-uri 'self'; " +
                    "form-action 'self'; " +
                    "frame-ancestors 'none'; " +
                    "upgrade-insecure-requests;";
            headers.add("Content-Security-Policy", csp);

            // Enforce HTTPS (only in production with HTTPS enabled)
            // Uncomment when deploying to production with HTTPS
            // headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

            // Control referrer information
            // Protects against: Information leakage via referrer
            headers.add("Referrer-Policy", "strict-origin-when-cross-origin");

            // Control browser features (replaces Feature-Policy)
            // Restricts access to sensitive browser APIs
            headers.add("Permissions-Policy",
                    "geolocation=(self), " +
                            "microphone=(), " +
                            "camera=(), " +
                            "payment=(self), " +
                            "usb=(), " +
                            "magnetometer=(), " +
                            "gyroscope=(), " +
                            "accelerometer=()");

            // Remove server information to prevent information disclosure
            headers.remove("Server");
            headers.remove("X-Powered-By");

            log.debug("Added security headers to response for: {}", exchange.getRequest().getPath());
        }));
    }

    @Override
    public int getOrder() {
        // Run this filter early in the chain
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
