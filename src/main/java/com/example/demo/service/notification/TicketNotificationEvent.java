package com.example.demo.service.notification;

public record TicketNotificationEvent(String recipient, String subject, String body) {
}
