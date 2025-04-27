package com.foodDelivery.restaurantService.repository;

import com.foodDelivery.restaurantService.model.CuisineType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuisineTypeRepository extends MongoRepository<CuisineType, String> {
    Optional<CuisineType> findByName(String name);
    List<CuisineType> findByActiveTrue();
}