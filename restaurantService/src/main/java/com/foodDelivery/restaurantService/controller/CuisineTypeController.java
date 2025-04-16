package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.dto.CuisineTypeCreateRequest;
import com.foodDelivery.restaurantService.dto.CuisineTypeResponse;
import com.foodDelivery.restaurantService.dto.CuisineTypeUpdateRequest;
import com.foodDelivery.restaurantService.serviceInterfaces.CuisineTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<CuisineTypeResponse> createCuisineType(@RequestBody CuisineTypeCreateRequest request) {
        log.info("Creating new cuisine type: {}", request.getName());
        CuisineTypeResponse created = cuisineTypeService.createCuisineType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CuisineTypeResponse> updateCuisineType(
            @PathVariable String id,
            @RequestBody CuisineTypeUpdateRequest request) {
        log.info("Updating cuisine type with id: {}", id);
        try {
            CuisineTypeResponse updated = cuisineTypeService.updateCuisineType(id, request);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating cuisine type: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCuisineType(@PathVariable String id) {
        log.info("Deleting cuisine type with id: {}", id);
        try {
            cuisineTypeService.deleteCuisineType(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error deleting cuisine type: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{cuisineTypeId}/restaurants/{restaurantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> addRestaurantToCuisineType(
            @PathVariable String cuisineTypeId,
            @PathVariable String restaurantId) {
        log.info("Adding restaurant {} to cuisine type {}", restaurantId, cuisineTypeId);
        try {
            cuisineTypeService.addRestaurantToCuisineType(cuisineTypeId, restaurantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error adding restaurant to cuisine type: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{cuisineTypeId}/restaurants/{restaurantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeRestaurantFromCuisineType(
            @PathVariable String cuisineTypeId,
            @PathVariable String restaurantId) {
        log.info("Removing restaurant {} from cuisine type {}", restaurantId, cuisineTypeId);
        try {
            cuisineTypeService.removeRestaurantFromCuisineType(cuisineTypeId, restaurantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error removing restaurant from cuisine type: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}