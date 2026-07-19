package com.example.demo.repository;

import com.example.demo.model.Faq;
import com.example.demo.model.PublicationStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Composable {@link Specification}s for FAQ search. Each returns a predicate (or
 * null, which Spring Data treats as "no constraint"), so the service can chain
 * only the filters that were actually supplied.
 */
public final class FaqSpecifications {

    private FaqSpecifications() {
    }

    public static Specification<Faq> statusIs(PublicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Faq> categoryNameIs(String categoryName) {
        return (root, query, cb) -> {
            if (categoryName == null || categoryName.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("category").get("name")), categoryName.trim().toLowerCase());
        };
    }

    public static Specification<Faq> hasTag(String tagName) {
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

    /** Case-insensitive partial match across the question and the answer body. */
    public static Specification<Faq> keyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            Predicate inQuestion = cb.like(cb.lower(root.get("question")), like);
            Predicate inAnswer = cb.like(cb.lower(root.get("answer")), like);
            return cb.or(inQuestion, inAnswer);
        };
    }
}
