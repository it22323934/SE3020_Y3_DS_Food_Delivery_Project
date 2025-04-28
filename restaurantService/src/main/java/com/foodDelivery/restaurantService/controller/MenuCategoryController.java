package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.MenuCategoryRequest;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.mapper.MenuCategoryTypeMapper;
import com.foodDelivery.restaurantService.model.MenuCategory;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuCategoryService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants/menu-categories")
@RequiredArgsConstructor
@Slf4j
public class MenuCategoryController {

    private static final String MENU_SERVICE = "menuService";
    private final MenuCategoryService menuCategoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = MENU_SERVICE, fallbackMethod = "createCategoryFallback")
    public ResponseEntity<?> createCategory(
            @Valid @RequestBody MenuCategoryRequest request,
            @RequestHeader("Authorization") String token) {
        MenuCategory created = menuCategoryService.createCategory(request, token);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MenuCategoryTypeMapper.mapToResponse(created));
    }

    public ResponseEntity<?> createCategoryFallback(
            MenuCategoryRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to create category", e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = MENU_SERVICE, fallbackMethod = "updateCategoryFallback")
    public ResponseEntity<?> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody MenuCategoryRequest request,
            @RequestHeader("Authorization") String token) {
        MenuCategory updated = menuCategoryService.updateCategory(id, request, token);
        return ResponseEntity.ok(MenuCategoryTypeMapper.mapToResponse(updated));
    }

    public ResponseEntity<?> updateCategoryFallback(
            String id, MenuCategoryRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to update category {}", id, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = MENU_SERVICE, fallbackMethod = "deleteCategoryFallback")
    public ResponseEntity<?> deleteCategory(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        menuCategoryService.deleteCategory(id, token);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> deleteCategoryFallback(String id, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to delete category {}", id, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @GetMapping("/{id}")
    @CircuitBreaker(name = MENU_SERVICE, fallbackMethod = "getCategoryByIdFallback")
    public ResponseEntity<?> getCategoryById(@PathVariable String id) {
        MenuCategory category = menuCategoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    public ResponseEntity<?> getCategoryByIdFallback(String id, Exception e) {
        log.error("Circuit breaker fallback: Failed to get category {}", id, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @GetMapping("/by-restaurant/{restaurantId}")
    @CircuitBreaker(name = MENU_SERVICE, fallbackMethod = "getCategoriesByRestaurantIdFallback")
    public ResponseEntity<List<MenuCategory>> getCategoriesByRestaurantId(
            @PathVariable String restaurantId) {
        List<MenuCategory> categories = menuCategoryService.getCategoriesByRestaurantId(restaurantId);
        return ResponseEntity.ok(categories);
    }

    public ResponseEntity<List<MenuCategory>> getCategoriesByRestaurantIdFallback(
            String restaurantId, Exception e) {
        log.error("Circuit breaker fallback: Failed to get categories for restaurant {}",
                restaurantId, e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @PutMapping("/reorder/{restaurantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = MENU_SERVICE, fallbackMethod = "reorderCategoriesFallback")
    public ResponseEntity<?> reorderCategories(
            @PathVariable String restaurantId,
            @RequestBody Map<String, List<String>> request,
            @RequestHeader("Authorization") String token) {
        if (!request.containsKey("categoryIds")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing categoryIds field"));
        }
        menuCategoryService.reorderCategories(restaurantId, request.get("categoryIds"), token);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> reorderCategoriesFallback(
            String restaurantId, Map<String, List<String>> request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to reorder categories for restaurant {}",
                restaurantId, e);
        if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }
}