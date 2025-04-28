package com.foodDelivery.restaurantService.repository;

import com.foodDelivery.restaurantService.model.Promotion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends MongoRepository<Promotion, String> {
    List<Promotion> findByRestaurantId(String restaurantId);
    List<Promotion> findByRestaurantIdAndActiveIsTrueAndStartDateBeforeAndEndDateAfter(
            String restaurantId, LocalDateTime now, LocalDateTime now2);
    Optional<Promotion> findByCodeAndRestaurantId(String code, String restaurantId);
}