package com.foodDelivery.paymentService.serviceImpl;

import com.foodDelivery.paymentService.model.PaymentEntity;
import com.foodDelivery.paymentService.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("paymentAuthorizationService")
@RequiredArgsConstructor
@Slf4j
public class PaymentAuthorizationService {

    private final PaymentRepository paymentRepository;

    /**
     * Check if the authenticated user can access payment information
     * @param email Email associated with the payment
     * @param authentication Current user's authentication
     * @return true if user can access, false otherwise
     */
    public boolean canAccessUserPayments(String email, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Unauthorized access attempt to payments for email: {}", email);
            return false;
        }

        // Admin can access all payments
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Users can only access their own payments
        String authenticatedUser = authentication.getName();
        boolean canAccess = authenticatedUser.equals(email);

        if (!canAccess) {
            log.warn("User {} attempted to access payments for {}", authenticatedUser, email);
        }

        return canAccess;
    }

    /**
     * Check if user can access a specific payment by order ID
     * @param orderId Order ID
     * @param authentication Current user's authentication
     * @return true if user can access, false otherwise
     */
    public boolean canAccessPaymentByOrderId(String orderId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            log.warn("Unauthorized access attempt to payment for order: {}", orderId);
            return false;
        }

        // Admin can access all payments
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return true;
        }

        // Check if payment belongs to the authenticated user
        PaymentEntity payment = paymentRepository.findByOrderId(orderId);
        if (payment == null) {
            log.warn("Payment not found for order: {}", orderId);
            return false;
        }

        String authenticatedUser = authentication.getName();
        boolean canAccess = authenticatedUser.equals(payment.getEmail());

        if (!canAccess) {
            log.warn("User {} attempted to access payment for order {} belonging to {}",
                    authenticatedUser, orderId, payment.getEmail());
        }

        return canAccess;
    }
}
