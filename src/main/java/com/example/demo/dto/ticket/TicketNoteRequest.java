package com.example.demo.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketNoteRequest(@NotBlank @Size(max = 1000) String message) {
}
