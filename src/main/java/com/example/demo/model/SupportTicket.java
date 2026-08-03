package com.example.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_ticket_reference", columnList = "referenceNumber", unique = true),
        @Index(name = "idx_ticket_customer", columnList = "customerEmail"),
        @Index(name = "idx_ticket_status", columnList = "status"),
        @Index(name = "idx_ticket_target", columnList = "targetResolutionAt")
})
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String referenceNumber;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, length = 150)
    private String project;

    private String spocEmail;
    private String department;
    private String assigneeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_article_id")
    private KnowledgeArticle suggestedArticle;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_session_id", unique = true)
    private DiagnosticSession diagnosticSession;

    @Column(nullable = false)
    private boolean automaticResolutionAttempted;

    @Column(nullable = false)
    private LocalDateTime targetResolutionAt;

    private LocalDateTime firstRespondedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TicketTimelineEvent> timeline = new ArrayList<>();

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadedAt ASC")
    private List<TicketAttachment> attachments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getProject() { return project; }
    public void setProject(String project) { this.project = project; }
    public String getSpocEmail() { return spocEmail; }
    public void setSpocEmail(String spocEmail) { this.spocEmail = spocEmail; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAssigneeEmail() { return assigneeEmail; }
    public void setAssigneeEmail(String assigneeEmail) { this.assigneeEmail = assigneeEmail; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public KnowledgeArticle getSuggestedArticle() { return suggestedArticle; }
    public void setSuggestedArticle(KnowledgeArticle suggestedArticle) { this.suggestedArticle = suggestedArticle; }
    public DiagnosticSession getDiagnosticSession() { return diagnosticSession; }
    public void setDiagnosticSession(DiagnosticSession diagnosticSession) { this.diagnosticSession = diagnosticSession; }
    public boolean isAutomaticResolutionAttempted() { return automaticResolutionAttempted; }
    public void setAutomaticResolutionAttempted(boolean automaticResolutionAttempted) { this.automaticResolutionAttempted = automaticResolutionAttempted; }
    public LocalDateTime getTargetResolutionAt() { return targetResolutionAt; }
    public void setTargetResolutionAt(LocalDateTime targetResolutionAt) { this.targetResolutionAt = targetResolutionAt; }
    public LocalDateTime getFirstRespondedAt() { return firstRespondedAt; }
    public void setFirstRespondedAt(LocalDateTime firstRespondedAt) { this.firstRespondedAt = firstRespondedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public List<TicketTimelineEvent> getTimeline() { return timeline; }
    public void setTimeline(List<TicketTimelineEvent> timeline) { this.timeline = timeline; }
    public List<TicketAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<TicketAttachment> attachments) { this.attachments = attachments; }
}
