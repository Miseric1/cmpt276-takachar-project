package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * An immutable snapshot of a {@link Faq} as it existed before an edit. One row
 * is written each time an FAQ's content changes, preserving the full history of
 * who changed what and when. This lays the groundwork for future audit and
 * revision-approval features without ever overwriting past knowledge.
 */
@Entity
@Table(name = "faq_versions", indexes = @Index(name = "idx_faq_version_faq", columnList = "faqId"))
public class FaqVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The FAQ this snapshot belongs to (id kept flat to decouple from deletes). */
    @Column(nullable = false)
    private Long faqId;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    private String categoryName;

    @Column(length = 20)
    private String status;

    private String editedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime editedAt;

    public FaqVersion() {
        this.editedAt = LocalDateTime.now();
    }

    /** Build a snapshot capturing the current state of an FAQ. */
    public static FaqVersion snapshotOf(Faq faq) {
        FaqVersion v = new FaqVersion();
        v.faqId = faq.getId();
        v.versionNumber = faq.getVersion();
        v.question = faq.getQuestion();
        v.answer = faq.getAnswer();
        v.categoryName = faq.getCategory() != null ? faq.getCategory().getName() : null;
        v.status = faq.getStatus() != null ? faq.getStatus().name() : null;
        v.editedBy = faq.getLastModifiedBy() != null ? faq.getLastModifiedBy() : faq.getCreatedBy();
        return v;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFaqId() {
        return faqId;
    }

    public void setFaqId(Long faqId) {
        this.faqId = faqId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(String editedBy) {
        this.editedBy = editedBy;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
}
