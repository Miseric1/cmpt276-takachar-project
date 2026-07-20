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
 * Immutable snapshot of a {@link KnowledgeArticle} before an edit, preserving
 * the full revision history (who changed what, and when).
 */
@Entity
@Table(name = "article_versions", indexes = @Index(name = "idx_article_version_article", columnList = "articleId"))
public class KnowledgeArticleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long articleId;

    @Column(nullable = false)
    private int versionNumber;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 1000)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private String categoryName;

    @Column(length = 20)
    private String status;

    private String editedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime editedAt;

    public KnowledgeArticleVersion() {
        this.editedAt = LocalDateTime.now();
    }

    public static KnowledgeArticleVersion snapshotOf(KnowledgeArticle article) {
        KnowledgeArticleVersion v = new KnowledgeArticleVersion();
        v.articleId = article.getId();
        v.versionNumber = article.getVersion();
        v.title = article.getTitle();
        v.summary = article.getSummary();
        v.body = article.getBody();
        v.categoryName = article.getCategory() != null ? article.getCategory().getName() : null;
        v.status = article.getStatus() != null ? article.getStatus().name() : null;
        v.editedBy = article.getLastModifiedBy() != null ? article.getLastModifiedBy() : article.getAuthor();
        return v;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
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
