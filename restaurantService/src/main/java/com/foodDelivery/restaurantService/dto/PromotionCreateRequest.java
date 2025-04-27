package com.foodDelivery.restaurantService.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PromotionCreateRequest {
    @NotBlank(message = "Restaurant ID is required")
    private String restaurantId;
    
    @NotBlank(message = "Promotion code is required")
    @Pattern(regexp = "^[A-Z0-9]{4,10}$", message = "Code must be 4-10 characters, uppercase letters and numbers only")
    private String code;
    
    @NotBlank(message = "Description is required")
    @Size(max = 200)
    private String description;
    
    @NotNull(message = "Discount percentage is required")
    @Min(0) @Max(100)
    private Double discountPercentage;
    
    @NotNull(message = "Maximum discount is required")
    @Min(0)
    private Double maxDiscount;
    
    @NotNull(message = "Minimum order amount is required")
    @Min(0)
    private Double minOrderAmount;
    
    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;
    
    @NotNull(message = "End date is required")
    private LocalDateTime endDate;
    
    @NotNull(message = "Maximum uses must be specified")
    @Min(1)
    private Integer maxUses;
    
    private boolean oneTimeUsePerUser;
}