package com.foodDelivery.notificationService.client;

import com.foodDelivery.notificationService.config.FeignClientConfig;
import com.foodDelivery.notificationService.dto.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "userService",
        url = "${user.service.url}",
        configuration = FeignClientConfig.class
)
public interface UserServiceClient {
    @GetMapping("/api/users/user/{userId}")
    UserProfileResponse getUserById(@PathVariable("userId") String userId);
}