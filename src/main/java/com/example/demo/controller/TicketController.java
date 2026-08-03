package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.ticket.TicketAssignmentRequest;
import com.example.demo.dto.ticket.TicketAttachmentDto;
import com.example.demo.dto.ticket.TicketCreateRequest;
import com.example.demo.dto.ticket.TicketNoteRequest;
import com.example.demo.dto.ticket.TicketResponse;
import com.example.demo.dto.ticket.TicketStatusUpdateRequest;
import com.example.demo.dto.ticket.TicketSummary;
import com.example.demo.model.TicketPriority;
import com.example.demo.model.TicketStatus;
import com.example.demo.service.AttachmentDownload;
import com.example.demo.service.TicketAttachmentService;
import com.example.demo.service.TicketService;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;
    private final TicketAttachmentService attachmentService;

    public TicketController(TicketService ticketService, TicketAttachmentService attachmentService) {
        this.ticketService = ticketService;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public PageResponse<TicketSummary> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String spoc,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @PageableDefault(size = 20, sort = "targetResolutionAt",
                    direction = org.springframework.data.domain.Sort.Direction.ASC)
            Pageable pageable,
            Authentication auth) {
        return ticketService.search(keyword, status, priority, project, department, spoc,
                createdFrom, createdTo, pageable, auth.getName(), isAdmin(auth));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketCreateRequest request,
                                                 Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.create(request, auth.getName(), isAdmin(auth)));
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, Authentication auth) {
        return ticketService.get(id, auth.getName(), isAdmin(auth));
    }

    @PatchMapping("/{id}/assignment")
    public TicketResponse assign(@PathVariable Long id,
                                 @Valid @RequestBody TicketAssignmentRequest request,
                                 Authentication auth) {
        return ticketService.assign(id, request, auth.getName());
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable Long id,
                                       @Valid @RequestBody TicketStatusUpdateRequest request,
                                       Authentication auth) {
        return ticketService.updateStatus(id, request, auth.getName(), isAdmin(auth));
    }

    @PostMapping("/{id}/timeline")
    public TicketResponse addNote(@PathVariable Long id,
                                  @Valid @RequestBody TicketNoteRequest request,
                                  Authentication auth) {
        return ticketService.addNote(id, request.message(), auth.getName(), isAdmin(auth));
    }

    @PostMapping(path = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketAttachmentDto> upload(@PathVariable Long id,
                                                      @RequestPart("file") MultipartFile file,
                                                      Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.add(id, file, auth.getName(), isAdmin(auth)));
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable Long id, @PathVariable Long attachmentId,
                                             Authentication auth) {
        AttachmentDownload download = attachmentService.download(id, attachmentId,
                auth.getName(), isAdmin(auth));
        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(download.contentType());
        } catch (Exception ex) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.filename()).build().toString())
                .body(download.resource());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId,
                                                 Authentication auth) {
        attachmentService.delete(id, attachmentId, auth.getName(), isAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
