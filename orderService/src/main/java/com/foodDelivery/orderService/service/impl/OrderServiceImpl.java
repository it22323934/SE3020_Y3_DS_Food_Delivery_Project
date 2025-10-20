package com.foodDelivery.orderService.service.impl;

import com.foodDelivery.orderService.client.RestaurantServiceClient;
import com.foodDelivery.orderService.client.UserServiceClient;
import com.foodDelivery.orderService.dto.*;
import com.foodDelivery.orderService.exception.BusinessValidationException;
import com.foodDelivery.orderService.exception.OrderNotFoundException;
import com.foodDelivery.orderService.exception.RestaurantNotFoundException;
import com.foodDelivery.orderService.mapper.OrderMapper;
import com.foodDelivery.orderService.model.ContactInfo;
import com.foodDelivery.orderService.model.DeliveryAddress;
import com.foodDelivery.orderService.model.Order;
import com.foodDelivery.orderService.model.OrderStatus;
import com.foodDelivery.orderService.repository.OrderRepository;
import com.foodDelivery.orderService.service.KafkaProducerService;
import com.foodDelivery.orderService.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantServiceClient restaurantServiceClient;
    private final UserServiceClient userServiceClient;
    private final OrderMapper orderMapper;
    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String token) {
        log.info("Creating order for restaurant: {}", request.getRestaurantId());
        
        // Verify restaurant exists and is active
        com.foodDelivery.orderService.dto.restaurant.RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(
                request.getRestaurantId(), token);
        
        if (restaurant == null) {
            throw new RestaurantNotFoundException("Restaurant not found: " + request.getRestaurantId());
        }
        
        if (!restaurant.isEnabled()) {
            throw new IllegalStateException("Restaurant is not active: " + request.getRestaurantId());
        }

        Order order = orderMapper.toEntity(request);
        order = orderRepository.save(order);

        // Publish order created event to Kafka
        kafkaProducerService.sendOrderCreatedEvent(order, request);
        
        log.info("Order created successfully with ID: {}", order.getId());
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    /**
     * Update order status.
     * SECURITY FIX: Added restaurant ownership verification
     */
    public OrderResponse updateOrderStatus(String orderId, OrderStatus status, String token) {
        log.info("Updating order status: {} for order: {}", status, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // SECURITY FIX: Verify user is admin of the restaurant that owns this order
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // Check if user is ADMIN (system admin can update any order)
        boolean isSystemAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isSystemAdmin) {
            // For RESTAURANT_ADMIN, verify they own the restaurant
            try {
                // Fetch restaurant details to check admin ownership
                com.foodDelivery.orderService.dto.restaurant.RestaurantResponse restaurant =
                    restaurantServiceClient.getRestaurantById(order.getRestaurantId(), token);

                // Get user ID from user service
                Long userId = userServiceClient.getUserIdFromToken(token);

                boolean isRestaurantAdmin = restaurant.getAdminIds().contains(String.valueOf(userId));

                if (!isRestaurantAdmin) {
                    log.warn("User {} attempted to update order {} for restaurant {} without permission",
                            currentUsername, orderId, order.getRestaurantId());
                    throw new SecurityException("You don't have permission to update orders for this restaurant");
                }

                log.info("User {} verified as admin of restaurant {}", currentUsername, order.getRestaurantId());
            } catch (SecurityException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to verify restaurant ownership: {}", e.getMessage());
                throw new BusinessValidationException("Failed to verify restaurant ownership");
            }
        }

        validateStatusTransition(order.getStatus(), status);

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        order = orderRepository.save(order);

        if (status == OrderStatus.READY_FOR_PICKUP) {
            // Convert Order to OrderCreateRequest
            OrderCreateRequest orderDetails = OrderCreateRequest.builder()
                    .userId(order.getUserId())
                    .restaurantId(order.getRestaurantId())
                    .items(order.getItems().stream()
                            .map(item -> OrderItemRequest.builder()
                                    .itemId(item.getItemId())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList()))
                    .contactInfo(mapContactInfo(order.getContactInfo()))
                    .deliveryAddress(mapDeliveryAddress(order.getDeliveryAddress()))
                    .deliveryInstructions(order.getDeliveryInstructions())
                    .paymentMethod(order.getPaymentMethod())
                    .subtotal(order.getSubtotal())
                    .taxAmount(order.getTaxAmount())
                    .deliveryFee(order.getDeliveryFee())
                    .discount(order.getDiscount())
                    .total(order.getTotal())
                    .deliveryLocation(mapLocation(order.getDeliveryLocation()))
                    .restaurantLocation(mapLocation(order.getRestaurantLocation()))
                    .promotion(mapPromotion(order.getPromotion()))
                    .build();

            kafkaProducerService.sendOrderOutForDeliveryEvent(order, orderDetails);
        } else {
            kafkaProducerService.sendOrderStatusUpdateEvent(order,status);
        }

        log.info("Order status updated successfully for order: {}", orderId);
        return orderMapper.toResponse(order);
    }

    private ContactInfoRequest mapContactInfo(ContactInfo contactInfo) {
        return ContactInfoRequest.builder()
                .name(contactInfo.getName())
                .phone(contactInfo.getPhone())
                .email(contactInfo.getEmail())
                .build();
    }

    private DeliveryAddressRequest mapDeliveryAddress(DeliveryAddress address) {
        return DeliveryAddressRequest.builder()
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .build();
    }

    private LocationRequest mapLocation(Order.Location location) {
        if (location == null) return null;
        return LocationRequest.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .address(location.getAddress())
                .name(location.getName())
                .build();
    }

    private PromotionDetailsRequest mapPromotion(Order.PromotionDetails promotion) {
        if (promotion == null) return null;
        return PromotionDetailsRequest.builder()
                .code(promotion.getCode())
                .discountAmount(promotion.getDiscountAmount())
                .build();
    }

    @Override
    public OrderResponse getOrderById(String orderId, String token) {
        log.info("Fetching order details for ID: {}", orderId);
        
        return orderRepository.findById(orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId, String token) {
        log.info("Fetching orders for user: {}", userId);
        
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByRestaurantId(String restaurantId, String token) {
        log.info("Fetching orders for restaurant: {}", restaurantId);
        
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId, String token) {
        log.info("Cancelling order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        if (!canCancel(order.getStatus())) {
            throw new IllegalStateException("Cannot cancel order in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Publish order cancelled event to Kafka
        kafkaProducerService.sendOrderStatusUpdateEvent(order, OrderStatus.CANCELLED);
        
        log.info("Order cancelled successfully: {}", orderId);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Add validation logic for status transitions
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update cancelled order");
        }
        
        if (currentStatus == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot update delivered order");
        }
        
        // Add more specific transition rules as needed
    }

    private boolean canCancel(OrderStatus status) {
        return status == OrderStatus.PENDING || 
               status == OrderStatus.CONFIRMED;
    }

    private void validateOrderRequest(OrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessValidationException("Order must contain at least one item");
        }

        if (request.getDeliveryLocation() == null) {
            throw new BusinessValidationException("Delivery location is required");
        }

        if (request.getContactInfo() == null) {
            throw new BusinessValidationException("Contact information is required");
        }

        // Validate total calculation
        double calculatedTotal = request.getSubtotal() + request.getTaxAmount() +
                request.getDeliveryFee() - request.getDiscount();
        if (Math.abs(calculatedTotal - request.getTotal()) > 0.01) {
            throw new BusinessValidationException("Invalid order total calculation");
        }
    }
}