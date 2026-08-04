package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@Entity
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;

    @Enumerated(EnumType.STRING)
    @jakarta.persistence.Column(nullable = false, length = 20,
            columnDefinition = "varchar(20) default 'FEEDBACK'")
    private SubmissionType type;
    
    private String project;
    
    private String account;
    
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String description;
    
    private String status;
    
    private String createdBy;

    /**
     * Email of the admin who logged this entry on a customer's behalf.
     * Null for submissions the customer made themselves.
     */
    private String loggedBy;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private SentimentLabel sentiment;

    private Double sentimentConfidence;

    private String sentimentModel;

    private LocalDateTime sentimentAnalyzedAt;

    public Feedback() {
        this.status = "OPEN"; // Default status
        this.type = SubmissionType.FEEDBACK;
    }

    public Feedback(String category, String project, String account, String description, String createdBy) {
        this(category, project, account, description, createdBy, SubmissionType.FEEDBACK);
    }

    public Feedback(String category, String project, String account, String description,
                    String createdBy, SubmissionType type) {
        this.category = category;
        this.project = project;
        this.account = account;
        this.description = description;
        this.createdBy = createdBy;
        this.status = "OPEN";
        this.type = type == null ? SubmissionType.FEEDBACK : type;
    }

    @PrePersist
    protected void onCreate() {
        if (this.type == null) this.type = SubmissionType.FEEDBACK;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public SubmissionType getType() {
        return type;
    }

    public void setType(SubmissionType type) {
        this.type = type;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public SentimentLabel getSentiment() {
        return sentiment;
    }

    public void setSentiment(SentimentLabel sentiment) {
        this.sentiment = sentiment;
    }

    public Double getSentimentConfidence() {
        return sentimentConfidence;
    }

    public void setSentimentConfidence(Double sentimentConfidence) {
        this.sentimentConfidence = sentimentConfidence;
    }

    public String getSentimentModel() {
        return sentimentModel;
    }

    public void setSentimentModel(String sentimentModel) {
        this.sentimentModel = sentimentModel;
    }

    public LocalDateTime getSentimentAnalyzedAt() {
        return sentimentAnalyzedAt;
    }

    public void setSentimentAnalyzedAt(LocalDateTime sentimentAnalyzedAt) {
        this.sentimentAnalyzedAt = sentimentAnalyzedAt;
    }
}
