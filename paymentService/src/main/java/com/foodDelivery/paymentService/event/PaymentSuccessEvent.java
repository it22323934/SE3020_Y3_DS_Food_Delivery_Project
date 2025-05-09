package com.foodDelivery.paymentService.event;

import java.time.LocalDateTime;

public class PaymentSuccessEvent {
    private String orderId;
    private String customerEmail;

    private double amount;
    private LocalDateTime paymentDate;

    // Constructors, getters, setters
    public PaymentSuccessEvent() {}

    public PaymentSuccessEvent(String orderId, String customerEmail, double amount, LocalDateTime paymentDate) {
        this.orderId = orderId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }
}