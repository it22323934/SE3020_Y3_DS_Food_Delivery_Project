package com.foodDelivery.orderService.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "restaurantService",
        url = "${restaurant.service.url}"
)
public interface RestaurantServiceClient {
    @GetMapping("/api/restaurants/{id}")
    @CircuitBreaker(name = "restaurantService")
    com.foodDelivery.orderService.dto.restaurant.RestaurantResponse getRestaurantById(@PathVariable("id") String id,
                                                                                      @RequestHeader("Authorization") String token);
}