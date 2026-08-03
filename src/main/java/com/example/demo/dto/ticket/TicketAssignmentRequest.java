package com.example.demo.dto.ticket;

import com.example.demo.model.TicketPriority;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TicketAssignmentRequest(
        @Size(max = 150) String department,
        @Email String assigneeEmail,
        @Email String spocEmail,
        TicketPriority priority,
        LocalDateTime targetResolutionAt) {
}
