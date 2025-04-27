package com.foodDelivery.restaurantService.client;

import com.foodDelivery.restaurantService.dto.user.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;

@FeignClient(
        name = "userService",
        url = "${userService.baseUrl}"
)
public interface UserServiceClient {

    @GetMapping("/api/users/user/{userId}")
    UserProfileResponse getAdminInfo(
            @PathVariable("userId") String userId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    );

    @GetMapping("/api/users/validate")
    Boolean validateUserRole(
            @RequestParam("userName") String userId,
            @RequestParam("role") String role,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    );

    @GetMapping("/api/users/validate")
    Boolean validateUserRoleById(
            @RequestParam("userId") String userId,
            @RequestParam("role") String role,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    );

    @GetMapping("/api/users/getUserId")
    Long getUserIdFromToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String token);
}