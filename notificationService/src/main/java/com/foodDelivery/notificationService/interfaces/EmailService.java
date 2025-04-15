package com.foodDelivery.notificationService.interfaces;

public interface EmailService {
    void sendEmail(String to, String subject, String content, boolean isHtml);
}
