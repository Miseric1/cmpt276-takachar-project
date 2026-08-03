package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.ticket.TicketAssignmentRequest;
import com.example.demo.dto.ticket.TicketCreateRequest;
import com.example.demo.dto.ticket.TicketResponse;
import com.example.demo.dto.ticket.TicketStatusUpdateRequest;
import com.example.demo.dto.ticket.TicketSummary;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.model.DiagnosticAnswer;
import com.example.demo.model.DiagnosticSession;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;
import com.example.demo.model.SupportTicket;
import com.example.demo.model.TicketEventType;
import com.example.demo.model.TicketPriority;
import com.example.demo.model.TicketStatus;
import com.example.demo.model.TicketTimelineEvent;
import com.example.demo.repository.KnowledgeArticleRepository;
import com.example.demo.repository.SupportTicketRepository;
import com.example.demo.repository.TicketSpecifications;
import com.example.demo.service.notification.TicketNotificationEvent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final SupportTicketRepository ticketRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final DiagnosticService diagnosticService;
    private final ApplicationEventPublisher eventPublisher;
    private final long targetResolutionHours;
    private final String defaultSpocEmail;

    public TicketService(SupportTicketRepository ticketRepository,
                         KnowledgeArticleRepository articleRepository,
                         DiagnosticService diagnosticService,
                         ApplicationEventPublisher eventPublisher,
                         @Value("${app.ticketing.target-resolution-hours:72}") long targetResolutionHours,
                         @Value("${app.ticketing.default-spoc-email:}") String defaultSpocEmail) {
        this.ticketRepository = ticketRepository;
        this.articleRepository = articleRepository;
        this.diagnosticService = diagnosticService;
        this.eventPublisher = eventPublisher;
        this.targetResolutionHours = targetResolutionHours;
        this.defaultSpocEmail = defaultSpocEmail == null ? "" : defaultSpocEmail.trim();
    }

    @Transactional
    public TicketResponse create(TicketCreateRequest request, String actor, boolean admin) {
        SupportTicket ticket = new SupportTicket();
        ticket.setReferenceNumber(generateReference());
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setCustomerEmail(admin && request.customerEmail() != null && !request.customerEmail().isBlank()
                ? request.customerEmail().trim() : actor);
        ticket.setProject(request.project().trim());
        ticket.setPriority(request.priority() == null ? TicketPriority.MEDIUM : request.priority());
        ticket.setSpocEmail(firstNonBlank(request.spocEmail(), defaultSpocEmail));
        ticket.setTargetResolutionAt(LocalDateTime.now().plusHours(Math.max(1, targetResolutionHours)));

        DiagnosticSession diagnostic = null;
        if (request.diagnosticSessionId() != null) {
            diagnostic = diagnosticService.escalate(request.diagnosticSessionId(), actor, admin);
            ticket.setDiagnosticSession(diagnostic);
            ticket.setCustomerEmail(diagnostic.getCustomerEmail());
            ticket.setSuggestedArticle(diagnostic.getSuggestedArticle());
        } else if (request.suggestedArticleId() != null) {
            ticket.setSuggestedArticle(resolvePublishedArticle(request.suggestedArticleId()));
        }
        ticket.setAutomaticResolutionAttempted(diagnostic != null || ticket.getSuggestedArticle() != null);

        addEvent(ticket, TicketEventType.CREATED, actor,
                "Ticket " + ticket.getReferenceNumber() + " created", null, TicketStatus.OPEN);
        SupportTicket saved = ticketRepository.save(ticket);
        publishCreated(saved);
        return TicketMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummary> search(String keyword, TicketStatus status, TicketPriority priority,
                                               String project, String department, String spoc,
                                               LocalDateTime createdFrom, LocalDateTime createdTo,
                                               Pageable pageable, String actor, boolean admin) {
        Specification<SupportTicket> spec = Specification.allOf(
                admin ? null : TicketSpecifications.ownedBy(actor),
                TicketSpecifications.keyword(keyword),
                TicketSpecifications.statusIs(status),
                TicketSpecifications.priorityIs(priority),
                TicketSpecifications.projectIs(project),
                TicketSpecifications.departmentIs(department),
                TicketSpecifications.spocIs(spoc),
                TicketSpecifications.createdBetween(createdFrom, createdTo));
        Page<SupportTicket> page = ticketRepository.findAll(spec, pageable);
        return PageResponse.of(page, TicketMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, String actor, boolean admin) {
        return TicketMapper.toResponse(findAccessible(id, actor, admin));
    }

    @Transactional
    public TicketResponse assign(Long id, TicketAssignmentRequest request, String actor) {
        SupportTicket ticket = find(id);
        String oldTarget = String.valueOf(ticket.getTargetResolutionAt());
        ticket.setDepartment(trimToNull(request.department()));
        ticket.setAssigneeEmail(trimToNull(request.assigneeEmail()));
        if (request.spocEmail() != null) ticket.setSpocEmail(trimToNull(request.spocEmail()));
        if (request.priority() != null) ticket.setPriority(request.priority());
        if (request.targetResolutionAt() != null) ticket.setTargetResolutionAt(request.targetResolutionAt());
        touchFirstResponse(ticket);

        addEvent(ticket, TicketEventType.ASSIGNED, actor,
                "Assigned to " + valueOrUnassigned(ticket.getDepartment())
                        + (ticket.getAssigneeEmail() == null ? "" : " (" + ticket.getAssigneeEmail() + ")"),
                null, null);
        if (request.targetResolutionAt() != null && !oldTarget.equals(String.valueOf(ticket.getTargetResolutionAt()))) {
            addEvent(ticket, TicketEventType.TARGET_DATE_CHANGED, actor,
                    "Target resolution date changed from " + oldTarget + " to " + ticket.getTargetResolutionAt(),
                    null, null);
        }
        SupportTicket saved = ticketRepository.save(ticket);
        if (saved.getAssigneeEmail() != null) {
            publish(saved.getAssigneeEmail(), "Ticket assigned: " + saved.getReferenceNumber(),
                    "You have been assigned " + saved.getReferenceNumber() + ": " + saved.getSubject());
        }
        return TicketMapper.toResponse(saved);
    }

    @Transactional
    public TicketResponse updateStatus(Long id, TicketStatusUpdateRequest request,
                                       String actor, boolean admin) {
        SupportTicket ticket = findAccessible(id, actor, admin);
        TicketStatus from = ticket.getStatus();

        if (!admin && !(from == TicketStatus.RESOLVED && request.status() == TicketStatus.CLOSED)) {
            throw new AccessDeniedException("Customers may only close their own resolved tickets.");
        }
        if (!from.canTransitionTo(request.status())) {
            throw new InvalidStateException("Cannot change ticket status from " + from + " to " + request.status() + ".");
        }
        if (admin && from.isOpen() && LocalDateTime.now().isAfter(ticket.getTargetResolutionAt())
                && request.status().isOpen() && (request.note() == null || request.note().isBlank())) {
            throw new InvalidStateException("An overdue ticket update requires a progress note or delay reason.");
        }

        ticket.setStatus(request.status());
        if (admin) touchFirstResponse(ticket);
        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (request.status() == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        } else if (from == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(null);
            ticket.setClosedAt(null);
        }

        String message = "Status changed from " + from + " to " + request.status();
        if (request.note() != null && !request.note().isBlank()) {
            message += ": " + request.note().trim();
        }
        addEvent(ticket, TicketEventType.STATUS_CHANGED, actor, message, from, request.status());
        SupportTicket saved = ticketRepository.save(ticket);
        publish(saved.getCustomerEmail(), "Ticket update: " + saved.getReferenceNumber(),
                message + "\n\n" + saved.getSubject());
        if (saved.getSpocEmail() != null && !saved.getSpocEmail().equalsIgnoreCase(saved.getCustomerEmail())) {
            publish(saved.getSpocEmail(), "Ticket update: " + saved.getReferenceNumber(),
                    message + "\n\n" + saved.getSubject());
        }
        return TicketMapper.toResponse(saved);
    }

    @Transactional
    public TicketResponse addNote(Long id, String message, String actor, boolean admin) {
        SupportTicket ticket = findAccessible(id, actor, admin);
        if (admin) touchFirstResponse(ticket);
        addEvent(ticket, TicketEventType.NOTE_ADDED, actor, message.trim(), null, null);
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public SupportTicket findAccessible(Long id, String actor, boolean admin) {
        SupportTicket ticket = find(id);
        if (!admin && !ticket.getCustomerEmail().equalsIgnoreCase(actor)) {
            throw new AccessDeniedException("You cannot access another customer's ticket.");
        }
        return ticket;
    }

    @Transactional
    public void recordAttachmentEvent(SupportTicket ticket, TicketEventType type,
                                      String filename, String actor) {
        addEvent(ticket, type, actor,
                (type == TicketEventType.ATTACHMENT_ADDED ? "Attached " : "Removed attachment ") + filename,
                null, null);
        ticketRepository.save(ticket);
    }

    private SupportTicket find(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    private KnowledgeArticle resolvePublishedArticle(Long id) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        if (article.getStatus() != PublicationStatus.PUBLISHED) {
            throw new InvalidStateException("Only published knowledge articles can be suggested on a ticket.");
        }
        return article;
    }

    private void addEvent(SupportTicket ticket, TicketEventType type, String actor, String message,
                          TicketStatus from, TicketStatus to) {
        TicketTimelineEvent event = new TicketTimelineEvent();
        event.setTicket(ticket);
        event.setType(type);
        event.setActorEmail(actor);
        event.setMessage(message);
        event.setFromStatus(from);
        event.setToStatus(to);
        ticket.getTimeline().add(event);
    }

    private void touchFirstResponse(SupportTicket ticket) {
        if (ticket.getFirstRespondedAt() == null) ticket.setFirstRespondedAt(LocalDateTime.now());
    }

    private String generateReference() {
        String prefix = "TKT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String reference;
        do {
            reference = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (ticketRepository.existsByReferenceNumber(reference));
        return reference;
    }

    private void publishCreated(SupportTicket ticket) {
        String trail = diagnosticTrail(ticket.getDiagnosticSession());
        publish(ticket.getCustomerEmail(), "Support ticket created: " + ticket.getReferenceNumber(),
                "We received your ticket " + ticket.getReferenceNumber() + ".\nTarget resolution: "
                        + ticket.getTargetResolutionAt() + "\n\n" + ticket.getSubject());
        publish(ticket.getSpocEmail(), "New support ticket: " + ticket.getReferenceNumber(),
                ticket.getSubject() + "\nCustomer: " + ticket.getCustomerEmail() + "\nProject: "
                        + ticket.getProject() + "\n\n" + ticket.getDescription() + trail);
    }

    private String diagnosticTrail(DiagnosticSession session) {
        if (session == null || session.getAnswers().isEmpty()) return "";
        StringBuilder trail = new StringBuilder("\n\nDiagnostic trail:");
        for (DiagnosticAnswer answer : session.getAnswers()) {
            trail.append("\n- ").append(answer.getQuestionPrompt()).append(": ")
                    .append(answer.getOptionLabel() != null ? answer.getOptionLabel() : answer.getAnswerText());
        }
        return trail.toString();
    }

    private void publish(String recipient, String subject, String body) {
        if (recipient != null && !recipient.isBlank()) {
            eventPublisher.publishEvent(new TicketNotificationEvent(recipient, subject, body));
        }
    }

    private String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String valueOrUnassigned(String value) {
        return value == null ? "unassigned" : value;
    }
}
