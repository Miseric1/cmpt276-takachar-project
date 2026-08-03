package com.example.demo.dto.ticket;

import java.time.LocalDateTime;

public record TicketAttachmentDto(
        Long id,
        String filename,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        LocalDateTime uploadedAt,
        String downloadUrl) {
}
