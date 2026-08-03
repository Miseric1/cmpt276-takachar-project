package com.example.demo.service;

import com.example.demo.dto.ticket.TicketAttachmentDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.SupportTicket;
import com.example.demo.model.TicketAttachment;
import com.example.demo.model.TicketEventType;
import com.example.demo.repository.TicketAttachmentRepository;
import com.example.demo.service.storage.AttachmentStorage;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Service
public class TicketAttachmentService {
    private static final Logger log = LoggerFactory.getLogger(TicketAttachmentService.class);
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final AttachmentStorage storage;
    private final long maxAttachmentBytes;

    public TicketAttachmentService(TicketAttachmentRepository attachmentRepository,
                                   TicketService ticketService,
                                   AttachmentStorage storage,
                                   @Value("${app.ticketing.max-attachment-bytes:26214400}") long maxAttachmentBytes) {
        this.attachmentRepository = attachmentRepository;
        this.ticketService = ticketService;
        this.storage = storage;
        this.maxAttachmentBytes = maxAttachmentBytes;
    }

    @Transactional
    public TicketAttachmentDto add(Long ticketId, MultipartFile file, String actor, boolean admin) {
        SupportTicket ticket = ticketService.findAccessible(ticketId, actor, admin);
        validate(file);
        String key = storage.store(file);
        cleanupUploadOnRollback(key);
        try {
            TicketAttachment attachment = new TicketAttachment();
            attachment.setTicket(ticket);
            attachment.setStorageKey(key);
            attachment.setOriginalFilename(safeFilename(file.getOriginalFilename()));
            attachment.setContentType(file.getContentType().toLowerCase(Locale.ROOT));
            attachment.setSizeBytes(file.getSize());
            attachment.setUploadedBy(actor);
            ticket.getAttachments().add(attachment);
            TicketAttachment saved = attachmentRepository.save(attachment);
            ticketService.recordAttachmentEvent(ticket, TicketEventType.ATTACHMENT_ADDED,
                    saved.getOriginalFilename(), actor);
            return toDto(saved);
        } catch (RuntimeException ex) {
            storage.delete(key);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long ticketId, Long attachmentId, String actor, boolean admin) {
        ticketService.findAccessible(ticketId, actor, admin);
        TicketAttachment attachment = find(ticketId, attachmentId);
        return new AttachmentDownload(storage.load(attachment.getStorageKey()),
                attachment.getOriginalFilename(), attachment.getContentType(), attachment.getSizeBytes());
    }

    @Transactional
    public void delete(Long ticketId, Long attachmentId, String actor, boolean admin) {
        SupportTicket ticket = ticketService.findAccessible(ticketId, actor, admin);
        TicketAttachment attachment = find(ticketId, attachmentId);
        ticket.getAttachments().removeIf(item -> item.getId().equals(attachmentId));
        ticketService.recordAttachmentEvent(ticket, TicketEventType.ATTACHMENT_REMOVED,
                attachment.getOriginalFilename(), actor);
        deleteAfterCommit(attachment.getStorageKey());
    }

    private TicketAttachment find(Long ticketId, Long attachmentId) {
        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new ResourceNotFoundException("Attachment", attachmentId);
        }
        return attachment;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image or video file is required.");
        }
        if (file.getSize() > maxAttachmentBytes) {
            throw new IllegalArgumentException("Attachment exceeds the configured size limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.toLowerCase(Locale.ROOT).startsWith("image/")
                || contentType.toLowerCase(Locale.ROOT).startsWith("video/"))) {
            throw new IllegalArgumentException("Only image and video attachments are supported.");
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "attachment";
        String normalized = filename.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String safe = (slash >= 0 ? normalized.substring(slash + 1) : normalized).replaceAll("[\\r\\n]", "_");
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    private TicketAttachmentDto toDto(TicketAttachment attachment) {
        return new TicketAttachmentDto(attachment.getId(), attachment.getOriginalFilename(),
                attachment.getContentType(), attachment.getSizeBytes(), attachment.getUploadedBy(),
                attachment.getUploadedAt(), "/api/tickets/" + attachment.getTicket().getId()
                        + "/attachments/" + attachment.getId());
    }

    private void cleanupUploadOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) safelyDelete(key);
            }
        });
    }

    private void deleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safelyDelete(key);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { safelyDelete(key); }
        });
    }

    private void safelyDelete(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Could not clean up ticket attachment {}: {}", key, ex.getMessage());
        }
    }
}
