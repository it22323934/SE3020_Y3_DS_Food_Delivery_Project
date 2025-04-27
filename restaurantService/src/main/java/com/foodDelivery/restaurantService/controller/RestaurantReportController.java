package com.foodDelivery.restaurantService.controller;

import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants/reports")
@RequiredArgsConstructor
@Slf4j
public class RestaurantReportController {

    private final RestaurantReportService reportService;

    @GetMapping("/{restaurantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT_ADMIN')")
    public ResponseEntity<byte[]> generateReport(
            @PathVariable String restaurantId,
            @RequestHeader("Authorization") String token) {
        try {
            byte[] pdfContent = reportService.generateRestaurantReport(restaurantId, token);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename",
                    "restaurant-report-" + restaurantId + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            log.error("Failed to generate restaurant report: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}