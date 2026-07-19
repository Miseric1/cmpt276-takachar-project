package com.example.demo.repository;

import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Composable {@link Specification}s for Knowledge Base search. Each returns null
 * when its filter is absent, so only supplied filters are applied.
 */
public final class KnowledgeSpecifications {

    private KnowledgeSpecifications() {
    }

    public static Specification<KnowledgeArticle> statusIs(PublicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<KnowledgeArticle> categoryNameIs(String categoryName) {
        return (root, query, cb) -> {
            if (categoryName == null || categoryName.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("category").get("name")), categoryName.trim().toLowerCase());
        };
    }

    public static Specification<KnowledgeArticle> hasTag(String tagName) {
        return (root, query, cb) -> {
            if (tagName == null || tagName.isBlank()) {
                return null;
            }
            if (query != null) {
                query.distinct(true);
            }
            Join<Object, Object> tags = root.join("tags");
            return cb.equal(cb.lower(tags.get("name")), tagName.trim().toLowerCase());
        };
    }

    public static Specification<KnowledgeArticle> authorIs(String author) {
        return (root, query, cb) -> {
            if (author == null || author.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("author")), author.trim().toLowerCase());
        };
    }

    /** Case-insensitive partial match across title, summary, and body. */
    public static Specification<KnowledgeArticle> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            Predicate inTitle = cb.like(cb.lower(root.get("title")), like);
            Predicate inSummary = cb.like(cb.lower(cb.coalesce(root.get("summary"), "")), like);
            Predicate inBody = cb.like(cb.lower(root.get("body")), like);
            return cb.or(inTitle, inSummary, inBody);
        };
    }
}
