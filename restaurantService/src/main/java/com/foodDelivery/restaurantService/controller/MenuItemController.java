package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.MenuItemCreateRequest;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuItemService;
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
@RequestMapping("/api/restaurants/menu-items")
@RequiredArgsConstructor
@Slf4j
public class MenuItemController {

    private static final String MENU_ITEM_SERVICE = "menuItemService";
    private final MenuItemService menuItemService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = MENU_ITEM_SERVICE, fallbackMethod = "createMenuItemFallback")
    public ResponseEntity<?> createMenuItem(
            @Valid @RequestBody MenuItemCreateRequest request,
            @RequestHeader("Authorization") String token) {
        MenuItemResponse created = menuItemService.createMenuItem(request, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    public ResponseEntity<?> createMenuItemFallback(
            MenuItemCreateRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to create menu item", e);
        if (e instanceof BusinessValidationException || e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = MENU_ITEM_SERVICE, fallbackMethod = "updateMenuItemFallback")
    public ResponseEntity<?> updateMenuItem(
            @PathVariable String id,
            @Valid @RequestBody MenuItemCreateRequest request,
            @RequestHeader("Authorization") String token) {
        MenuItemResponse updated = menuItemService.updateMenuItem(id, request, token);
        return ResponseEntity.ok(updated);
    }

    public ResponseEntity<?> updateMenuItemFallback(
            String id, MenuItemCreateRequest request, String token, Exception e) {
        log.error("Circuit breaker fallback: Failed to update menu item {}", id, e);
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } else if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @GetMapping("/{id}")
    @CircuitBreaker(name = MENU_ITEM_SERVICE, fallbackMethod = "getMenuItemByIdFallback")
    public ResponseEntity<?> getMenuItemById(@PathVariable String id) {
        MenuItemResponse menuItem = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(menuItem);
    }

    public ResponseEntity<?> getMenuItemByIdFallback(String id, Exception e) {
        log.error("Circuit breaker fallback: Failed to get menu item {}", id, e);
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }

    @GetMapping("/by-restaurant/{restaurantId}")
    @CircuitBreaker(name = MENU_ITEM_SERVICE, fallbackMethod = "getMenuItemsByRestaurantIdFallback")
    public ResponseEntity<?> getMenuItemsByRestaurantId(@PathVariable String restaurantId) {
        List<MenuItemResponse> menuItems = menuItemService.getMenuItemsByRestaurantId(restaurantId);
        return ResponseEntity.ok(menuItems);
    }

    public ResponseEntity<?> getMenuItemsByRestaurantIdFallback(String restaurantId, Exception e) {
        log.error("Circuit breaker fallback: Failed to get menu items for restaurant {}", restaurantId, e);
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    @GetMapping("/by-category/{categoryId}")
    @CircuitBreaker(name = MENU_ITEM_SERVICE, fallbackMethod = "getMenuItemsByCategoryIdFallback")
    public ResponseEntity<?> getMenuItemsByCategoryId(@PathVariable String categoryId) {
        List<MenuItemResponse> menuItems = menuItemService.getMenuItemsByCategoryId(categoryId);
        return ResponseEntity.ok(menuItems);
    }

    public ResponseEntity<?> getMenuItemsByCategoryIdFallback(String categoryId, Exception e) {
        log.error("Circuit breaker fallback: Failed to get menu items for category {}", categoryId, e);
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.emptyList());
    }

    /**
     * Delete menu item.
     * SECURITY: Fixed - Added authorization token to verify ownership
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    @CircuitBreaker(name = MENU_ITEM_SERVICE, fallbackMethod = "deleteMenuItemFallback")
    public ResponseEntity<?> deleteMenuItem(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        menuItemService.deleteMenuItem(id, token);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<?> deleteMenuItemFallback(String id, Exception e) {
        log.error("Circuit breaker fallback: Failed to delete menu item {}", id, e);
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } else if (e instanceof BusinessValidationException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Service temporarily unavailable"));
    }
}