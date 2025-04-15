package com.foodDelivery.notificationService.interfaces;

public interface SmsService {
    void sendSms(String phoneNumber, String message);
}