package com.foodDelivery.orderService.mapper;

import com.foodDelivery.orderService.dto.*;
import com.foodDelivery.orderService.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public Order toEntity(OrderCreateRequest request) {
        return Order.builder()
                .userId(request.getUserId())
                .restaurantId(request.getRestaurantId())
                .items(request.getItems().stream()
                        .map(this::toOrderItem)
                        .collect(Collectors.toList()))
                .contactInfo(toContactInfo(request.getContactInfo()))
                .deliveryAddress(toDeliveryAddress(request.getDeliveryAddress()))
                .deliveryInstructions(request.getDeliveryInstructions())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .subtotal(request.getSubtotal())
                .taxAmount(request.getTaxAmount())
                .deliveryFee(request.getDeliveryFee())
                .discount(request.getDiscount())
                .total(request.getTotal())
                .deliveryLocation(toLocation(request.getDeliveryLocation()))
                .restaurantLocation(toLocation(request.getRestaurantLocation()))
                .promotion(toPromotionDetails(request.getPromotion()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .restaurantId(order.getRestaurantId())
                .items(order.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList()))
                .contactInfo(toContactInfoResponse(order.getContactInfo()))
                .deliveryAddress(toDeliveryAddressResponse(order.getDeliveryAddress()))
                .deliveryInstructions(order.getDeliveryInstructions())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .deliveryFee(order.getDeliveryFee())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .deliveryLocation(toLocationResponse(order.getDeliveryLocation()))
                .restaurantLocation(toLocationResponse(order.getRestaurantLocation()))
                .promotion(toPromotionDetailsResponse(order.getPromotion()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private Order.Location toLocation(LocationRequest request) {
        if (request == null) return null;
        return Order.Location.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .name(request.getName())
                .build();
    }

    private LocationResponse toLocationResponse(Order.Location location) {
        if (location == null) return null;
        return LocationResponse.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .address(location.getAddress())
                .name(location.getName())
                .build();
    }

    private OrderItem toOrderItem(OrderItemRequest request) {
        return OrderItem.builder()
                .itemId(request.getItemId())
                .name(request.getName())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .itemTotal(request.getItemTotal())
                .addOns(request.getAddOns() != null ?
                        request.getAddOns().stream()
                                .map(this::toOrderItemAddOn)
                                .collect(Collectors.toList()) :
                        new ArrayList<>())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .itemId(item.getItemId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .itemTotal(item.getItemTotal())
                .addOns(item.getAddOns().stream()
                        .map(this::toAddOnResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private Order.PromotionDetails toPromotionDetails(PromotionDetailsRequest request) {
        if (request == null) return null;
        return Order.PromotionDetails.builder()
                .code(request.getCode())
                .discountAmount(request.getDiscountAmount())
                .build();
    }

    private PromotionDetailsResponse toPromotionDetailsResponse(Order.PromotionDetails details) {
        if (details == null) return null;
        return PromotionDetailsResponse.builder()
                .code(details.getCode())
                .discountAmount(details.getDiscountAmount())
                .build();
    }

    private OrderItemAddOn toOrderItemAddOn(OrderItemAddOnRequest request) {
        return OrderItemAddOn.builder()
                .addOnId(request.getAddOnId())
                .name(request.getName())
                .price(request.getPrice())
                .build();
    }

    private OrderItemAddOnResponse toAddOnResponse(OrderItemAddOn addOn) {
        return OrderItemAddOnResponse.builder()
                .addOnId(addOn.getAddOnId())
                .name(addOn.getName())
                .price(addOn.getPrice())
                .build();
    }

    private ContactInfo toContactInfo(ContactInfoRequest request) {
        return ContactInfo.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();
    }

    private ContactInfoResponse toContactInfoResponse(ContactInfo info) {
        return ContactInfoResponse.builder()
                .name(info.getName())
                .email(info.getEmail())
                .phone(info.getPhone())
                .build();
    }

    private DeliveryAddress toDeliveryAddress(DeliveryAddressRequest request) {
        return DeliveryAddress.builder()
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .build();
    }

    private DeliveryAddressResponse toDeliveryAddressResponse(DeliveryAddress address) {
        return DeliveryAddressResponse.builder()
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .build();
    }
}