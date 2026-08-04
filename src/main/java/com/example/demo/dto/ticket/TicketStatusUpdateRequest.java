package com.example.demo.dto.ticket;

import com.example.demo.model.TicketStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketStatusUpdateRequest(
        @NotNull TicketStatus status,
        @Size(max = 1000) String note) {
}
