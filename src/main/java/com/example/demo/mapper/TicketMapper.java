package com.example.demo.mapper;

import com.example.demo.dto.knowledge.ArticleReference;
import com.example.demo.dto.ticket.TicketAttachmentDto;
import com.example.demo.dto.ticket.TicketResponse;
import com.example.demo.dto.ticket.TicketSummary;
import com.example.demo.dto.ticket.TicketTimelineEventDto;
import com.example.demo.model.SupportTicket;
import com.example.demo.model.TicketHealth;
import com.example.demo.model.TicketStatus;

import java.time.LocalDateTime;

public final class TicketMapper {
    private TicketMapper() {
    }

    public static TicketSummary toSummary(SupportTicket ticket) {
        return new TicketSummary(ticket.getId(), ticket.getReferenceNumber(), ticket.getSubject(),
                ticket.getCustomerEmail(), ticket.getProject(), ticket.getDepartment(),
                ticket.getAssigneeEmail(), ticket.getStatus(), ticket.getPriority(), health(ticket),
                ticket.getTargetResolutionAt(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    public static TicketResponse toResponse(SupportTicket ticket) {
        return new TicketResponse(
                ticket.getId(), ticket.getReferenceNumber(), ticket.getSubject(), ticket.getDescription(),
                ticket.getCustomerEmail(), ticket.getProject(), ticket.getSpocEmail(), ticket.getDepartment(),
                ticket.getAssigneeEmail(), ticket.getStatus(), ticket.getPriority(), health(ticket),
                ticket.getSuggestedArticle() == null ? null : ArticleReference.from(ticket.getSuggestedArticle()),
                ticket.isAutomaticResolutionAttempted(),
                ticket.getDiagnosticSession() == null ? null : ticket.getDiagnosticSession().getId(),
                ticket.getDiagnosticSession() == null ? java.util.List.of()
                        : ticket.getDiagnosticSession().getAnswers().stream().map(DiagnosticMapper::toTrail).toList(),
                ticket.getAttachments().stream().map(attachment -> new TicketAttachmentDto(
                        attachment.getId(), attachment.getOriginalFilename(), attachment.getContentType(),
                        attachment.getSizeBytes(), attachment.getUploadedBy(), attachment.getUploadedAt(),
                        "/api/tickets/" + ticket.getId() + "/attachments/" + attachment.getId())).toList(),
                ticket.getTimeline().stream().map(event -> new TicketTimelineEventDto(
                        event.getId(), event.getType(), event.getActorEmail(), event.getMessage(),
                        event.getFromStatus(), event.getToStatus(), event.getCreatedAt())).toList(),
                ticket.getTargetResolutionAt(), ticket.getFirstRespondedAt(), ticket.getResolvedAt(),
                ticket.getClosedAt(), ticket.getCreatedAt(), ticket.getUpdatedAt());
    }

    public static TicketHealth health(SupportTicket ticket) {
        LocalDateTime target = ticket.getTargetResolutionAt();
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            return ticket.getResolvedAt() != null && target != null && ticket.getResolvedAt().isAfter(target)
                    ? TicketHealth.RED : TicketHealth.GREEN;
        }
        return target != null && LocalDateTime.now().isAfter(target) ? TicketHealth.RED : TicketHealth.YELLOW;
    }
}
