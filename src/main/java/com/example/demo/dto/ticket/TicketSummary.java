package com.example.demo.dto.ticket;

import com.example.demo.model.TicketHealth;
import com.example.demo.model.TicketPriority;
import com.example.demo.model.TicketStatus;

import java.time.LocalDateTime;

public record TicketSummary(
        Long id,
        String referenceNumber,
        String subject,
        String customerEmail,
        String project,
        String department,
        String assigneeEmail,
        TicketStatus status,
        TicketPriority priority,
        TicketHealth health,
        LocalDateTime targetResolutionAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
