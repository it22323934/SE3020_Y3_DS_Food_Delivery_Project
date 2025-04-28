package com.foodDelivery.notificationService.client;

import com.foodDelivery.notificationService.config.FeignClientConfig;
import com.foodDelivery.notificationService.dto.CuisineTypeResponse;
import com.foodDelivery.notificationService.dto.RestaurantResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "restaurantService",
        url = "${restaurant.service.url}",
        configuration = FeignClientConfig.class
)
public interface RestaurantServiceClient {
    @GetMapping("/api/restaurants/cuisine-types/{id}")
    CuisineTypeResponse getCuisineTypeById(@PathVariable("id") String cuisineId);

    @GetMapping("/api/restaurants/{id}")
    RestaurantResponse getRestaurantById(@PathVariable("id") String id);

}