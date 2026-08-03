package com.example.demo.dto.ticket;

import com.example.demo.model.TicketEventType;
import com.example.demo.model.TicketStatus;

import java.time.LocalDateTime;

public record TicketTimelineEventDto(
        Long id,
        TicketEventType type,
        String actorEmail,
        String message,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        LocalDateTime createdAt) {
}
