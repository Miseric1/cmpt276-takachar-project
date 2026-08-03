package com.example.demo.dto.ticket;

import com.example.demo.dto.diagnostic.DiagnosticTrailDto;
import com.example.demo.dto.knowledge.ArticleReference;
import com.example.demo.model.TicketHealth;
import com.example.demo.model.TicketPriority;
import com.example.demo.model.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        Long id,
        String referenceNumber,
        String subject,
        String description,
        String customerEmail,
        String project,
        String spocEmail,
        String department,
        String assigneeEmail,
        TicketStatus status,
        TicketPriority priority,
        TicketHealth health,
        ArticleReference suggestedArticle,
        boolean automaticResolutionAttempted,
        UUID diagnosticSessionId,
        List<DiagnosticTrailDto> diagnosticTrail,
        List<TicketAttachmentDto> attachments,
        List<TicketTimelineEventDto> timeline,
        LocalDateTime targetResolutionAt,
        LocalDateTime firstRespondedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
