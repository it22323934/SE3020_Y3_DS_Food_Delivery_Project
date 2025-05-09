package com.foodDelivery.notificationService.service;

import com.foodDelivery.notificationService.client.RestaurantServiceClient;
import com.foodDelivery.notificationService.client.UserServiceClient;
import com.foodDelivery.notificationService.dto.RestaurantResponse;
import com.foodDelivery.notificationService.dto.UserProfileResponse;
import com.foodDelivery.notificationService.interfaces.EmailService;
import com.foodDelivery.notificationService.interfaces.SmsService;
import com.foodDelivery.orderService.event.OrderEvent;
import com.foodDelivery.orderService.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationService {

    private final EmailService emailService;
    private final SmsService smsService;
    private final UserServiceClient userServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private static final String ORDER_TOPIC = "order-notifications";

    @KafkaListener(topics = ORDER_TOPIC, containerFactory = "kafkaListenerContainerFactoryBroker3")
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: {}, type: {}, status: {}", formatOrderNumber(event.getOrderId()), event.getEventType(), event.getStatus());

        try {
            switch (event.getEventType()) {
                case "ORDER_CREATED":
                    processOrderCreatedEvent(event);
                    break;
                case "ORDER_STATUS_UPDATED":
                    processOrderStatusUpdateEvent(event);
                    break;
                case "ORDER_OUT_FOR_DELIVERY":
                    processOrderOutForDeliveryEvent(event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }

    private void processOrderCreatedEvent(OrderEvent event) {
        // Get user and restaurant details from respective services
        UserProfileResponse user = userServiceClient.getUserById(String.valueOf(event.getUserId()));
        RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(event.getRestaurantId());

        String formattedOrderNumber = formatOrderNumber(event.getOrderId());

        // Send notifications
        if (user != null && user.getEmail() != null) {
            String emailContent = createOrderCreatedEmailForUser(event, user.getFirstName());
            emailService.sendEmail(user.getEmail(),
                    "Your Order " + formattedOrderNumber + " Has Been Placed",
                    emailContent, true);

            // Send SMS to user
            if (user.getPhoneNumber() != null) {
                String smsContent = String.format(
                        "🍔 Your order %s has been placed with %s. Status: %s. Total: $%.2f. Track your order in the app.",
                        formattedOrderNumber, restaurant.getName(), event.getStatus(), event.getTotal()
                );
                smsService.sendSms(user.getPhoneNumber(), smsContent);
            }
        }

        // Notify restaurant
        if (restaurant != null && restaurant.getEmail() != null) {
            String emailContent = createOrderCreatedEmailForRestaurant(event, restaurant.getName());
            emailService.sendEmail(restaurant.getEmail(),
                    "New Order " + formattedOrderNumber + " Received",
                    emailContent, true);
        }
    }

    private void processOrderStatusUpdateEvent(OrderEvent event) {
        // Get user and restaurant details
        UserProfileResponse user = userServiceClient.getUserById(String.valueOf(event.getUserId()));
        String formattedOrderNumber = formatOrderNumber(event.getOrderId());

        // Send notification to user only
        if (user != null && user.getEmail() != null) {
            String emailContent = createOrderStatusUpdateEmail(event, user.getFirstName());
            emailService.sendEmail(user.getEmail(),
                    String.format("Order %s Update: %s", formattedOrderNumber, event.getStatus()),
                    emailContent, true);

            // Send SMS
            if (user.getPhoneNumber() != null) {
                String statusEmoji = getStatusEmoji(event.getStatus());
                String smsContent = String.format(
                        "%s Your order %s status: %s. %s",
                        statusEmoji, formattedOrderNumber, event.getStatus(),
                        getStatusSpecificMessage(event.getStatus())
                );
                smsService.sendSms(user.getPhoneNumber(), smsContent);
            }
        }

        // For certain statuses, notify the restaurant as well
        if (event.getStatus() == OrderStatus.CONFIRMED || event.getStatus() == OrderStatus.CANCELLED) {
            RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(event.getRestaurantId());
            if (restaurant != null && restaurant.getEmail() != null) {
                String emailContent = createRestaurantStatusUpdateEmail(event, restaurant.getName());
                emailService.sendEmail(restaurant.getEmail(),
                        String.format("Order %s Status Update: %s", formattedOrderNumber, event.getStatus()),
                        emailContent, true);
            }
        }
    }

    private String getStatusEmoji(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "✅";
            case PREPARING -> "👨‍🍳";
            case READY_FOR_PICKUP -> "📦";
            case OUT_FOR_DELIVERY -> "🚚";
            case DELIVERED -> "🎉";
            case CANCELLED -> "❌";
            default -> "🔄";
        };
    }

    private String getStatusSpecificMessage(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Your order has been confirmed. It will be prepared soon.";
            case PREPARING -> "The restaurant is now preparing your order.";
            case READY_FOR_PICKUP -> "Your order is ready for pickup by our delivery partner.";
            case OUT_FOR_DELIVERY -> "Your order is on its way to you!";
            case DELIVERED -> "Enjoy your meal! Thank you for ordering with us.";
            case CANCELLED -> "We're sorry for any inconvenience caused.";
            default -> "Check your email for details.";
        };
    }

    private void processOrderOutForDeliveryEvent(OrderEvent event) {
        // Get user details
        UserProfileResponse user = userServiceClient.getUserById(String.valueOf(event.getUserId()));
        String formattedOrderNumber = formatOrderNumber(event.getOrderId());

        // Send notification to user
        if (user != null && user.getEmail() != null) {
            String emailContent = createOrderOutForDeliveryEmail(event, user.getFirstName());
            emailService.sendEmail(user.getEmail(),
                    "Your Order " + formattedOrderNumber + " Is Out For Delivery",
                    emailContent, true);

            // Send SMS
            if (user.getPhoneNumber() != null) {
                String smsContent = String.format(
                        "🚚 Good news! Your order %s is out for delivery. Estimated delivery time: %s",
                        formattedOrderNumber, LocalDateTime.now().plusMinutes(30)
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                );
                smsService.sendSms(user.getPhoneNumber(), smsContent);
            }
        }
    }

    private String formatOrderNumber(String orderId) {
        // Extract last 6 chars if ID is long enough, otherwise use the whole ID
        String shortId = orderId.length() > 6 ? orderId.substring(orderId.length() - 6) : orderId;
        return "FD-" + shortId.toUpperCase();
    }

    private String createOrderCreatedEmailForUser(OrderEvent event, String userName) {
        String itemsList = event.getItems().stream()
                .map(item -> String.format("<li>%s x%d - $%.2f</li>",
                        item.getName(), item.getQuantity(), item.getPrice() * item.getQuantity()))
                .collect(Collectors.joining("\n"));

        String formattedOrderNumber = formatOrderNumber(event.getOrderId());

        return """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
                <h1 style="color: #2c3e50; margin-bottom: 10px;">Order Confirmation</h1>
                <p style="color: #7f8c8d; margin-bottom: 20px;">Hi %s, thank you for your order!</p>
            </div>
            
            <div style="background-color: #ffffff; border: 1px solid #e1e1e1; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <span style="display: inline-block; background-color: #e74c3c; color: white; font-size: 16px; font-weight: bold; padding: 8px 15px; border-radius: 20px;">
                        Order %s
                    </span>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Status:</strong>
                    <p style="color: %s; font-weight: bold; margin: 5px 0;">%s %s</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Items:</strong>
                    <ul style="color: #7f8c8d; margin: 5px 0;">
                        %s
                    </ul>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 15px;">
                    <div>
                        <strong style="color: #2c3e50;">Subtotal:</strong>
                        <p style="color: #7f8c8d; margin: 5px 0;">$%.2f</p>
                    </div>
                    <div>
                        <strong style="color: #2c3e50;">Tax:</strong>
                        <p style="color: #7f8c8d; margin: 5px 0;">$%.2f</p>
                    </div>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 15px;">
                    <div>
                        <strong style="color: #2c3e50;">Delivery Fee:</strong>
                        <p style="color: #7f8c8d; margin: 5px 0;">$%.2f</p>
                    </div>
                    <div>
                        <strong style="color: #2c3e50;">Discount:</strong>
                        <p style="color: #27ae60; margin: 5px 0;">-$%.2f</p>
                    </div>
                </div>
                <div style="margin-bottom: 15px; background-color: #f8f9fa; padding: 10px; border-radius: 5px;">
                    <strong style="color: #2c3e50;">Total:</strong>
                    <p style="color: #e74c3c; font-weight: bold; font-size: 18px; margin: 5px 0;">$%.2f</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Delivery Address:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s, %s, %s, %s</p>
                </div>
            </div>
            
            <div style="background-color: #cce5ff; padding: 15px; border-radius: 5px; text-align: center; border: 1px solid #b8daff;">
                <p style="color: #004085; margin: 0;">
                    Track your order status in real-time on our app.
                    <br>This is an automated message. Please do not reply.
                </p>
            </div>
        </div>
        """.formatted(
                userName,
                formattedOrderNumber,
                getStatusColor(event.getStatus()),
                event.getStatus(),
                getStatusEmoji(event.getStatus()),
                itemsList,
                event.getSubtotal(),
                event.getTaxAmount(),
                event.getDeliveryFee(),
                event.getDiscount(),
                event.getTotal(),
                event.getDeliveryAddress().getStreet(),
                event.getDeliveryAddress().getCity(),
                event.getDeliveryAddress().getState(),
                event.getDeliveryAddress().getZipCode()
        );
    }

    private String createOrderCreatedEmailForRestaurant(OrderEvent event, String restaurantName) {
        String itemsList = event.getItems().stream()
                .map(item -> String.format("<li>%s x%d</li>",
                        item.getName(), item.getQuantity()))
                .collect(Collectors.joining("\n"));

        String formattedOrderNumber = formatOrderNumber(event.getOrderId());

        return """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
                <h1 style="color: #2c3e50; margin-bottom: 10px;">New Order Received</h1>
                <p style="color: #7f8c8d; margin-bottom: 20px;">%s has received a new order!</p>
            </div>
            
            <div style="background-color: #ffffff; border: 1px solid #e1e1e1; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <span style="display: inline-block; background-color: #3498db; color: white; font-size: 16px; font-weight: bold; padding: 8px 15px; border-radius: 20px;">
                        Order %s
                    </span>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Status:</strong>
                    <p style="color: %s; font-weight: bold; margin: 5px 0;">%s %s</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Items:</strong>
                    <ul style="color: #7f8c8d; margin: 5px 0;">
                        %s
                    </ul>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Customer:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Contact:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">Phone: %s, Email: %s</p>
                </div>
                <div style="margin-bottom: 15px; background-color: #f8f9fa; padding: 10px; border-radius: 5px;">
                    <strong style="color: #2c3e50;">Total:</strong>
                    <p style="color: #e74c3c; font-weight: bold; font-size: 18px; margin: 5px 0;">$%.2f</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Special Instructions:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
                </div>
            </div>
            
            <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; text-align: center;">
                <p style="color: #7f8c8d; margin: 0;">Please prepare the order for pickup. This is an automated message.</p>
            </div>
        </div>
        """.formatted(
                restaurantName,
                formattedOrderNumber,
                getStatusColor(event.getStatus()),
                event.getStatus(),
                getStatusEmoji(event.getStatus()),
                itemsList,
                event.getContactInfo().getName(),
                event.getContactInfo().getPhone(),
                event.getContactInfo().getEmail(),
                event.getTotal(),
                event.getDeliveryInstructions() != null ? event.getDeliveryInstructions() : "None"
        );
    }

    private String createOrderStatusUpdateEmail(OrderEvent event, String userName) {
        String statusSpecificMessage = getStatusSpecificMessage(event.getStatus());
        String statusColor = getStatusColor(event.getStatus());
        String statusEmoji = getStatusEmoji(event.getStatus());
        String formattedOrderNumber = formatOrderNumber(event.getOrderId());

        return """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
                <h1 style="color: #2c3e50; margin-bottom: 10px;">Order Status Update</h1>
                <p style="color: #7f8c8d; margin-bottom: 20px;">Hi %s, your order status has been updated!</p>
            </div>
            
            <div style="background-color: #ffffff; border: 1px solid #e1e1e1; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <span style="display: inline-block; background-color: #3498db; color: white; font-size: 16px; font-weight: bold; padding: 8px 15px; border-radius: 20px;">
                        Order %s
                    </span>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">New Status:</strong>
                    <div style="background-color: #f8f9fa; border-radius: 5px; padding: 10px; margin: 5px 0;">
                        <span style="color: %s; font-weight: bold; font-size: 18px;">%s %s</span>
                    </div>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Status Details:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Updated At:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
                </div>
            </div>
            
            <div style="background-color: #cce5ff; padding: 15px; border-radius: 5px; text-align: center; border: 1px solid #b8daff;">
                <p style="color: #004085; margin: 0;">
                    Track your order status in real-time on our app.
                    <br>This is an automated message. Please do not reply.
                </p>
            </div>
        </div>
        """.formatted(
                userName,
                formattedOrderNumber,
                statusColor,
                event.getStatus(),
                statusEmoji,
                statusSpecificMessage,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
        );
    }

    private String createRestaurantStatusUpdateEmail(OrderEvent event, String restaurantName) {
        String actionRequired = "";
        if (event.getStatus() == OrderStatus.CONFIRMED) {
            actionRequired = "Please start preparing this order.";
        } else if (event.getStatus() == OrderStatus.CANCELLED) {
            actionRequired = "Please disregard this order.";
        }

        String formattedOrderNumber = formatOrderNumber(event.getOrderId());
        String statusEmoji = getStatusEmoji(event.getStatus());

        return """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
                <h1 style="color: #2c3e50; margin-bottom: 10px;">Order Status Update</h1>
                <p style="color: #7f8c8d; margin-bottom: 20px;">%s, an order status has been updated!</p>
            </div>
            
            <div style="background-color: #ffffff; border: 1px solid #e1e1e1; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <span style="display: inline-block; background-color: #3498db; color: white; font-size: 16px; font-weight: bold; padding: 8px 15px; border-radius: 20px;">
                        Order %s
                    </span>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Status:</strong>
                    <div style="background-color: #f8f9fa; border-radius: 5px; padding: 10px; margin: 5px 0;">
                        <span style="color: %s; font-weight: bold; font-size: 18px;">%s %s</span>
                    </div>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Action Required:</strong>
                    <p style="color: #e74c3c; font-weight: bold; margin: 5px 0;">%s</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Updated At:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s</p>
                </div>
            </div>
            
            <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; text-align: center;">
                <p style="color: #7f8c8d; margin: 0;">This is an automated message. Please do not reply.</p>
            </div>
        </div>
        """.formatted(
                restaurantName,
                formattedOrderNumber,
                getStatusColor(event.getStatus()),
                event.getStatus(),
                statusEmoji,
                actionRequired,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
        );
    }

    private String getStatusColor(OrderStatus status) {
        return switch (status) {
            case PENDING -> "#FFA500"; // Orange
            case CONFIRMED -> "#3498DB"; // Blue
            case PREPARING -> "#9B59B6"; // Purple
            case READY_FOR_PICKUP -> "#F1C40F"; // Yellow
            case OUT_FOR_DELIVERY -> "#2ECC71"; // Green
            case DELIVERED -> "#27AE60"; // Dark Green
            case CANCELLED -> "#E74C3C"; // Red
        };
    }

    private String createOrderOutForDeliveryEmail(OrderEvent event, String userName) {
        String formattedOrderNumber = formatOrderNumber(event.getOrderId());

        return """
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <div style="background-color: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 20px;">
                <h1 style="color: #2c3e50; margin-bottom: 10px;">Your Order Is On The Way! 🚚</h1>
                <p style="color: #7f8c8d; margin-bottom: 20px;">Hi %s, your food is out for delivery!</p>
            </div>
            
            <div style="background-color: #ffffff; border: 1px solid #e1e1e1; border-radius: 10px; padding: 20px; margin-bottom: 20px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <span style="display: inline-block; background-color: #2ECC71; color: white; font-size: 16px; font-weight: bold; padding: 8px 15px; border-radius: 20px;">
                        Order %s
                    </span>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Status:</strong>
                    <p style="color: #2ECC71; font-weight: bold; margin: 5px 0;">%s 🚚</p>
                </div>
                <div style="margin-bottom: 15px; background-color: #d4edda; padding: 10px; border-radius: 5px;">
                    <strong style="color: #155724;">Estimated Delivery:</strong>
                    <p style="color: #155724; font-weight: bold; font-size: 18px; margin: 5px 0;">%s</p>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong style="color: #2c3e50;">Delivery Address:</strong>
                    <p style="color: #7f8c8d; margin: 5px 0;">%s, %s, %s, %s</p>
                </div>
                <div style="border-top: 1px dashed #e1e1e1; margin: 15px 0; padding-top: 15px;">
                    <p style="color: #7f8c8d; font-style: italic;">Our delivery partner is on the way with your order. Please ensure someone is available to receive it.</p>
                </div>
            </div>
            
            <div style="background-color: #d4edda; padding: 15px; border-radius: 5px; text-align: center; border: 1px solid #c3e6cb;">
                <p style="color: #155724; margin: 0;">
                    Track your delivery driver in real-time on our app.
                    <br>This is an automated message. Please do not reply.
                </p>
            </div>
        </div>
        """.formatted(
                userName,
                formattedOrderNumber,
                event.getStatus(),
                LocalDateTime.now().plusMinutes(30).format(DateTimeFormatter.ofPattern("HH:mm")),
                event.getDeliveryAddress().getStreet(),
                event.getDeliveryAddress().getCity(),
                event.getDeliveryAddress().getState(),
                event.getDeliveryAddress().getZipCode()
        );
    }
}