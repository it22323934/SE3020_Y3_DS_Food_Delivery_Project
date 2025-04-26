package com.foodDelivery.restaurantService.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RestaurantReportRequest {
    private String restaurantId;
    private String reportType; // FULL, SUMMARY, MENU, PERFORMANCE
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}