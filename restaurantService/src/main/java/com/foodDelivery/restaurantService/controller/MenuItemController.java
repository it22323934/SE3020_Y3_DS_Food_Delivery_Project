package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.MenuItemCreateRequest;
import com.foodDelivery.restaurantService.dto.MenuItemResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.exception.ResourceNotFoundException;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuItemService;
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
@RequestMapping("/api/restaurants/menu-items")
@RequiredArgsConstructor
@Slf4j
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<?> createMenuItem(
            @Valid @RequestBody MenuItemCreateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            MenuItemResponse created = menuItemService.createMenuItem(request, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BusinessValidationException | ResourceNotFoundException e) {
            log.warn("Error creating menu item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMenuItemById(@PathVariable String id) {
        try {
            MenuItemResponse menuItem = menuItemService.getMenuItemById(id);
            return ResponseEntity.ok(menuItem);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/by-restaurant/{restaurantId}")
    public ResponseEntity<?> getMenuItemsByRestaurantId(@PathVariable String restaurantId) {
        try {
            List<MenuItemResponse> menuItems = menuItemService.getMenuItemsByRestaurantId(restaurantId);
            return ResponseEntity.ok(menuItems);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<?> getMenuItemsByCategoryId(@PathVariable String categoryId) {
        try {
            List<MenuItemResponse> menuItems = menuItemService.getMenuItemsByCategoryId(categoryId);
            return ResponseEntity.ok(menuItems);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}