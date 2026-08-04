package com.example.demo.dto.ticket;

import com.example.demo.model.TicketPriority;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TicketCreateRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank @Size(max = 10000) String description,
        @NotBlank @Size(max = 150) String project,
        @Email String customerEmail,
        TicketPriority priority,
        @Email String spocEmail,
        UUID diagnosticSessionId,
        Long suggestedArticleId) {
}
