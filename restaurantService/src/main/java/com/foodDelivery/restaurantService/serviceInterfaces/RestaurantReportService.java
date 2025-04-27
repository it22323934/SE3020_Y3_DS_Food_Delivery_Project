package com.foodDelivery.restaurantService.serviceInterfaces;

import com.foodDelivery.restaurantService.dto.RestaurantReportRequest;

public interface RestaurantReportService {
    byte[] generateRestaurantReport(String restaurantId, String token);
    byte[] generateCustomReport(RestaurantReportRequest request);
}