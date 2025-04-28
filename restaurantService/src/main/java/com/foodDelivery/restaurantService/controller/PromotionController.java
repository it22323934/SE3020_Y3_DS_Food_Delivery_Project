package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.PromotionCreateRequest;
import com.foodDelivery.restaurantService.dto.PromotionResponse;
import com.foodDelivery.restaurantService.dto.PromotionValidationRequest;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.serviceInterfaces.PromotionService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {

    private static final String PROMOTION_SERVICE = "promotionService";
    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = PROMOTION_SERVICE, fallbackMethod = "createPromotionFallback")
    public ResponseEntity<?> createPromotion(
            @Valid @RequestBody PromotionCreateRequest request,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionService.createPromotion(request, token));
    }

    public ResponseEntity<?> createPromotionFallback(
            PromotionCreateRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to create promotion", e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = PROMOTION_SERVICE, fallbackMethod = "updatePromotionFallback")
    public ResponseEntity<?> updatePromotion(
            @PathVariable String id,
            @Valid @RequestBody PromotionCreateRequest request,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, request, token));
    }

    public ResponseEntity<?> updatePromotionFallback(
            String id, PromotionCreateRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to update promotion {}", id, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = PROMOTION_SERVICE, fallbackMethod = "deletePromotionFallback")
    public ResponseEntity<?> deletePromotion(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        promotionService.deletePromotion(id, token);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> deletePromotionFallback(String id, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to delete promotion {}", id, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @PostMapping("/validate")
    @CircuitBreaker(name = PROMOTION_SERVICE, fallbackMethod = "validatePromotionFallback")
    public ResponseEntity<?> validatePromotion(
            @Valid @RequestBody PromotionValidationRequest request) {
        return ResponseEntity.ok(promotionService.validatePromotion(request));
    }

    public ResponseEntity<?> validatePromotionFallback(
            PromotionValidationRequest request, Exception e) {
        log.error("Circuit breaker fallback: Failed to validate promotion", e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @CircuitBreaker(name = PROMOTION_SERVICE, fallbackMethod = "getPromotionsByRestaurantIdFallback")
    public ResponseEntity<?> getPromotionsByRestaurantId(
            @PathVariable String restaurantId) {
        return ResponseEntity.ok(promotionService.getPromotionsByRestaurantId(restaurantId));
    }

    public ResponseEntity<?> getPromotionsByRestaurantIdFallback(
            String restaurantId, Exception e) {
        log.error("Circuit breaker fallback: Failed to get promotions for restaurant {}",
                restaurantId, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/restaurant/{restaurantId}/active")
    @CircuitBreaker(name = PROMOTION_SERVICE, fallbackMethod = "getActivePromotionsFallback")
    public ResponseEntity<?> getActivePromotions(
            @PathVariable String restaurantId) {
        return ResponseEntity.ok(promotionService.getActivePromotionsByRestaurantId(restaurantId));
    }

    public ResponseEntity<?> getActivePromotionsFallback(
            String restaurantId, Exception e) {
        log.error("Circuit breaker fallback: Failed to get active promotions for restaurant {}",
                restaurantId, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }
}