package com.example.demo.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diagnostic_sessions")
public class DiagnosticSession {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiagnosticSessionStatus status = DiagnosticSessionStatus.IN_PROGRESS;

    @Column(name = "current_node_id")
    private UUID currentNodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_article_id")
    private KnowledgeArticle suggestedArticle;

    @Column(length = 2000)
    private String suggestedResolution;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("answeredAt ASC")
    private List<DiagnosticAnswer> answers = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public DiagnosticSessionStatus getStatus() { return status; }
    public void setStatus(DiagnosticSessionStatus status) { this.status = status; }
    public UUID getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(UUID currentNodeId) { this.currentNodeId = currentNodeId; }
    public KnowledgeArticle getSuggestedArticle() { return suggestedArticle; }
    public void setSuggestedArticle(KnowledgeArticle suggestedArticle) { this.suggestedArticle = suggestedArticle; }
    public String getSuggestedResolution() { return suggestedResolution; }
    public void setSuggestedResolution(String suggestedResolution) { this.suggestedResolution = suggestedResolution; }
    public List<DiagnosticAnswer> getAnswers() { return answers; }
    public void setAnswers(List<DiagnosticAnswer> answers) { this.answers = answers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
