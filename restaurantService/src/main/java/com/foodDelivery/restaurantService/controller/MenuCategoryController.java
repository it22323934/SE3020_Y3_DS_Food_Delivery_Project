package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.MenuCategoryRequest;
import com.foodDelivery.restaurantService.dto.MenuCategoryResponse;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.mapper.MenuCategoryTypeMapper;
import com.foodDelivery.restaurantService.model.MenuCategory;
import com.foodDelivery.restaurantService.serviceInterfaces.MenuCategoryService;
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

    private final MenuCategoryService menuCategoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<MenuCategoryResponse> createCategory(
            @Valid @RequestBody MenuCategoryRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            MenuCategory created = menuCategoryService.createCategory(request, token);
            return ResponseEntity.status(HttpStatus.CREATED).body(MenuCategoryTypeMapper.mapToResponse(created));
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<MenuCategoryResponse> updateCategory(
            @PathVariable String id,
            @Valid @RequestBody MenuCategoryRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            MenuCategory updated = menuCategoryService.updateCategory(id, request, token);
            return ResponseEntity.status(HttpStatus.OK).body(MenuCategoryTypeMapper.mapToResponse(updated));
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<?> deleteCategory(
            @PathVariable String id,
            @RequestHeader("Authorization") String token) {
        try {
            menuCategoryService.deleteCategory(id, token);
            return ResponseEntity.noContent().build();
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuCategory> getCategoryById(@PathVariable String id) {
        try {
            MenuCategory category = menuCategoryService.getCategoryById(id);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/by-restaurant/{restaurantId}")
    public ResponseEntity<List<MenuCategory>> getCategoriesByRestaurantId(@PathVariable String restaurantId) {
        List<MenuCategory> categories = menuCategoryService.getCategoriesByRestaurantId(restaurantId);
        return ResponseEntity.ok(categories);
    }

    @PutMapping("/reorder/{restaurantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<?> reorderCategories(
            @PathVariable String restaurantId,
            @RequestBody Map<String, List<String>> request,
            @RequestHeader("Authorization") String token) {
        try {
            if (!request.containsKey("categoryIds")) {
                return ResponseEntity.badRequest().body("Missing categoryIds field");
            }

            menuCategoryService.reorderCategories(restaurantId, request.get("categoryIds"), token);
            return ResponseEntity.ok().build();
        } catch (BusinessValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }
}