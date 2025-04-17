package com.foodDelivery.userService.modal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class UserNotificationEvent {
    // Getters and setters
    @Getter
    private String email;
    private String eventType;
    @Getter
    private Map<String, Object> data;
    private Object timestamp; // Using Object to handle both Integer and Long

    // Default constructor required for deserialization
    public UserNotificationEvent() {
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    // Handle both Integer and Long for timestamp
    public long getTimestamp() {
        if (timestamp instanceof Integer) {
            return ((Integer) timestamp).longValue();
        } else if (timestamp instanceof Long) {
            return (Long) timestamp;
        }
        return 0L;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }
}