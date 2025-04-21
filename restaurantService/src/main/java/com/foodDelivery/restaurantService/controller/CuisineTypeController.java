package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.CuisineTypeCreateRequest;
import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.CuisineTypeUpdateRequest;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.serviceInterfaces.CuisineTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants/cuisine-types")
@RequiredArgsConstructor
@Slf4j
public class CuisineTypeController {

    private final CuisineTypeService cuisineTypeService;

    @GetMapping
    public ResponseEntity<List<CuisineTypeResponse>> getAllCuisineTypes() {
        log.info("Fetching all cuisine types");
        List<CuisineTypeResponse> cuisineTypes = cuisineTypeService.getAllCuisineTypes();
        return ResponseEntity.ok(cuisineTypes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CuisineTypeResponse> getCuisineTypeById(@PathVariable String id) {
        log.info("Fetching cuisine type with id: {}", id);
        return cuisineTypeService.getCuisineTypeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCuisineType(@RequestBody CuisineTypeCreateRequest request) {
        log.info("Creating new cuisine type: {}", request.getName());
        try {
            CuisineTypeResponse created = cuisineTypeService.createCuisineType(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BusinessValidationException | IllegalArgumentException e) {
            log.warn("Error creating cuisine type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCuisineType(
            @PathVariable String id,
            @RequestBody CuisineTypeUpdateRequest request) {
        log.info("Updating cuisine type with id: {}", id);
        try {
            CuisineTypeResponse updated = cuisineTypeService.updateCuisineType(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating cuisine type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCuisineType(@PathVariable String id) {
        log.info("Deleting cuisine type with id: {}", id);
        try {
            cuisineTypeService.deleteCuisineType(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting cuisine type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{cuisineTypeId}/restaurants/{restaurantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addRestaurantToCuisineType(
            @PathVariable String cuisineTypeId,
            @PathVariable String restaurantId) {
        log.info("Adding restaurant {} to cuisine type {}", restaurantId, cuisineTypeId);
        try {
            cuisineTypeService.addRestaurantToCuisineType(cuisineTypeId, restaurantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error adding restaurant to cuisine type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{cuisineTypeId}/restaurants/{restaurantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeRestaurantFromCuisineType(
            @PathVariable String cuisineTypeId,
            @PathVariable String restaurantId) {
        log.info("Removing restaurant {} from cuisine type {}", restaurantId, cuisineTypeId);
        try {
            cuisineTypeService.removeRestaurantFromCuisineType(cuisineTypeId, restaurantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error removing restaurant from cuisine type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}