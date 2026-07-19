package com.example.demo.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A Knowledge Base article: longer-form documentation such as guides,
 * troubleshooting procedures, and manuals. Richer than an {@link Faq} -- it
 * carries a summary, a long body, an author and contributor list, related
 * articles, and a computed reading time -- while sharing the same category,
 * tag, publication, and analytics infrastructure.
 */
@Entity
@Table(name = "knowledge_articles", indexes = {
        @Index(name = "idx_article_status", columnList = "status"),
        @Index(name = "idx_article_category", columnList = "category_id"),
        @Index(name = "idx_article_created_at", columnList = "createdAt")
})
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 1000)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "article_tags",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    /** Other articles a reader may want next. Self-referencing many-to-many. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "article_related",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "related_article_id"))
    private Set<KnowledgeArticle> relatedArticles = new LinkedHashSet<>();

    private String author;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "article_contributors", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "contributor")
    private Set<String> contributors = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicationStatus status = PublicationStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private int version = 1;

    /** Minutes, derived from the body word count when content changes. */
    @Column(nullable = false)
    private int estimatedReadingTimeMinutes = 1;

    private String lastModifiedBy;

    @Column(nullable = false)
    private long viewCount = 0;

    @Column(nullable = false)
    private long helpfulCount = 0;

    @Column(nullable = false)
    private long notHelpfulCount = 0;

    public KnowledgeArticle() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == PublicationStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = this.createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters ----------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Set<KnowledgeArticle> getRelatedArticles() {
        return relatedArticles;
    }

    public void setRelatedArticles(Set<KnowledgeArticle> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Set<String> getContributors() {
        return contributors;
    }

    public void setContributors(Set<String> contributors) {
        this.contributors = contributors;
    }

    public PublicationStatus getStatus() {
        return status;
    }

    public void setStatus(PublicationStatus status) {
        this.status = status;
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

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getEstimatedReadingTimeMinutes() {
        return estimatedReadingTimeMinutes;
    }

    public void setEstimatedReadingTimeMinutes(int estimatedReadingTimeMinutes) {
        this.estimatedReadingTimeMinutes = estimatedReadingTimeMinutes;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public long getHelpfulCount() {
        return helpfulCount;
    }

    public void setHelpfulCount(long helpfulCount) {
        this.helpfulCount = helpfulCount;
    }

    public long getNotHelpfulCount() {
        return notHelpfulCount;
    }

    public void setNotHelpfulCount(long notHelpfulCount) {
        this.notHelpfulCount = notHelpfulCount;
    }
}
