package com.foodDelivery.restaurantService.serviceImplementation;

import com.foodDelivery.restaurantService.client.UserServiceClient;
import com.foodDelivery.restaurantService.dto.PromotionCreateRequest;
import com.foodDelivery.restaurantService.dto.PromotionResponse;
import com.foodDelivery.restaurantService.dto.PromotionValidationRequest;
import com.foodDelivery.restaurantService.event.PromotionCreatedEvent;
import com.foodDelivery.restaurantService.exception.BusinessValidationException;
import com.foodDelivery.restaurantService.mapper.PromotionMapper;
import com.foodDelivery.restaurantService.model.Promotion;
import com.foodDelivery.restaurantService.model.Restaurant;
import com.foodDelivery.restaurantService.repository.PromotionRepository;
import com.foodDelivery.restaurantService.serviceInterfaces.KafkaProducerService;
import com.foodDelivery.restaurantService.serviceInterfaces.PromotionService;
import com.foodDelivery.restaurantService.serviceInterfaces.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final RestaurantService restaurantService;
    private final UserServiceClient userServiceClient;
    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public PromotionResponse createPromotion(PromotionCreateRequest request, String token) {
        validatePromotionRequest(request);
        Restaurant restaurant = restaurantService.getRestaurantById(request.getRestaurantId());
        validateUserPermission(restaurant, token);

        promotionRepository.findByCodeAndRestaurantId(request.getCode(), request.getRestaurantId())
                .ifPresent(p -> {
                    throw new BusinessValidationException("Promotion code already exists");
                });

        Promotion promotion = PromotionMapper.toEntity(request);
        promotion.setRestaurantName(restaurant.getName());
        Promotion saved = promotionRepository.save(promotion);

        kafkaProducerService.publishPromotionCreatedEvent(saved);
        return PromotionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(String id, PromotionCreateRequest request, String token) {
        Promotion existing = promotionRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Promotion not found"));

        Restaurant restaurant = restaurantService.getRestaurantById(existing.getRestaurantId());
        validateUserPermission(restaurant, token);
        validatePromotionRequest(request);
        validateOwnership(existing.getRestaurantId(), request.getRestaurantId());

        existing.setDescription(request.getDescription());
        existing.setDiscountPercentage(request.getDiscountPercentage());
        existing.setMaxDiscount(request.getMaxDiscount());
        existing.setMinOrderAmount(request.getMinOrderAmount());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());
        existing.setMaxUses(request.getMaxUses());
        existing.setOneTimeUsePerUser(request.isOneTimeUsePerUser());
        existing.setUpdatedAt(LocalDateTime.now());
        kafkaProducerService.publishPromotionUpdatedEvent(existing);
        return PromotionMapper.toResponse(promotionRepository.save(existing));
    }

    @Override
    @Transactional
    public void deletePromotion(String id, String token) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("Promotion not found"));

        Restaurant restaurant = restaurantService.getRestaurantById(promotion.getRestaurantId());
        validateUserPermission(restaurant, token);
        kafkaProducerService.publishPromotionDeletedEvent(promotion);
        promotionRepository.delete(promotion);
    }

    @Override
    public PromotionResponse getPromotionById(String id) {
        return promotionRepository.findById(id)
                .map(PromotionMapper::toResponse)
                .orElseThrow(() -> new BusinessValidationException("Promotion not found"));
    }

    @Override
    public List<PromotionResponse> getPromotionsByRestaurantId(String restaurantId) {
        return promotionRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(PromotionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PromotionResponse validatePromotion(PromotionValidationRequest request) {
        Promotion promotion = promotionRepository.findByCodeAndRestaurantId(
                request.getCode().toUpperCase(), request.getRestaurantId())
                .orElseThrow(() -> new BusinessValidationException("Invalid promotion code"));

        validatePromotionStatus(promotion, request);

        // If validation passes, increment usage and add user if one-time use
        if (promotion.isOneTimeUsePerUser()) {
            promotion.getUsedByUsers().add(request.getUserId());
        }
        promotion.setCurrentUses(promotion.getCurrentUses() + 1);
        promotionRepository.save(promotion);

        return PromotionMapper.toResponse(promotion);
    }

    @Override
    public List<PromotionResponse> getActivePromotionsByRestaurantId(String restaurantId) {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository
                .findByRestaurantIdAndActiveIsTrueAndStartDateBeforeAndEndDateAfter(
                        restaurantId, now, now)
                .stream()
                .map(PromotionMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void validatePromotionRequest(PromotionCreateRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BusinessValidationException("Start date must be before end date");
        }
        if (request.getEndDate().isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("End date must be in the future");
        }
        if (request.getMaxDiscount() <= 0) {
            throw new BusinessValidationException("Maximum discount must be greater than 0");
        }
        if (request.getMinOrderAmount() < 0) {
            throw new BusinessValidationException("Minimum order amount cannot be negative");
        }
    }

    private void validatePromotionStatus(Promotion promotion, PromotionValidationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        
        if (!promotion.isActive()) {
            throw new BusinessValidationException("Promotion is not active");
        }
        if (now.isBefore(promotion.getStartDate()) || now.isAfter(promotion.getEndDate())) {
            throw new BusinessValidationException("Promotion is not valid at this time");
        }
        if (promotion.getCurrentUses() >= promotion.getMaxUses()) {
            throw new BusinessValidationException("Promotion has reached maximum uses");
        }
        if (request.getOrderAmount() < promotion.getMinOrderAmount()) {
            throw new BusinessValidationException(
                    "Order amount does not meet minimum requirement of " + 
                    promotion.getMinOrderAmount());
        }
        if (promotion.isOneTimeUsePerUser() && 
            promotion.getUsedByUsers().contains(request.getUserId())) {
            throw new BusinessValidationException("Promotion has already been used by this user");
        }
    }

    private void validateOwnership(String existingRestaurantId, String requestRestaurantId) {
        if (!existingRestaurantId.equals(requestRestaurantId)) {
            throw new BusinessValidationException("Cannot modify promotion for different restaurant");
        }
    }

    private void validateUserPermission(Restaurant restaurant, String token) {
        if (token == null || token.trim().isEmpty()) {
            log.error("Authorization token is missing or empty");
            throw new BusinessValidationException("Authorization required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        log.debug("Validating permissions for user: {}", currentUserId);

        try {
            boolean isSystemAdmin = userServiceClient.validateUserRole(currentUserId, "ROLE_ADMIN", token);
            if (isSystemAdmin) {
                log.debug("User {} has system admin privileges", currentUserId);
                return;
            }

            Long userID = userServiceClient.getUserIdFromToken(token);
            if (userID == null) {
                log.error("User ID could not be extracted from token");
                throw new BusinessValidationException("Invalid authorization token");
            }

            boolean isRestaurantAdmin = userServiceClient.validateUserRole(
                    currentUserId, "ROLE_RESTAURANT_ADMIN", token);

            if (!isRestaurantAdmin) {
                log.warn("User {} is not a restaurant admin", currentUserId);
                throw new BusinessValidationException("You need restaurant administrator privileges");
            }

            if (!restaurant.getAdminIds().contains(String.valueOf(userID))) {
                log.warn("User {} is not an admin of restaurant {}", userID, restaurant.getId());
                throw new BusinessValidationException(
                        "You don't have administrator permissions for this restaurant");
            }
        } catch (Exception e) {
            log.error("Failed to validate user permissions: {}", e.getMessage());
            throw new BusinessValidationException("Failed to validate user authorization");
        }
    }
}