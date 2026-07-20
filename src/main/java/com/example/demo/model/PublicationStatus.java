package com.example.demo.model;

import java.util.Set;

/**
 * Lifecycle state shared by FAQs and Knowledge Base articles. Persisted as a
 * string (see {@code @Enumerated(EnumType.STRING)} on the entities) so the
 * stored value stays readable and stable even as new states are added.
 */
public enum PublicationStatus {

    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    HIDDEN,
    ARCHIVED,
    EXPIRED;

    /** Only PUBLISHED content is visible to customers. */
    public boolean isPubliclyVisible() {
        return this == PUBLISHED;
    }

    /**
     * Allowed transitions between states. Anything not listed here is rejected
     * by the service layer with an {@code InvalidStateException}.
     */
    private static final java.util.Map<PublicationStatus, Set<PublicationStatus>> ALLOWED = java.util.Map.of(
            DRAFT, Set.of(PENDING_REVIEW, PUBLISHED, ARCHIVED, HIDDEN),
            PENDING_REVIEW, Set.of(DRAFT, PUBLISHED, ARCHIVED, HIDDEN),
            PUBLISHED, Set.of(HIDDEN, ARCHIVED, EXPIRED, DRAFT),
            HIDDEN, Set.of(PUBLISHED, ARCHIVED, DRAFT),
            ARCHIVED, Set.of(DRAFT, PUBLISHED),
            EXPIRED, Set.of(PUBLISHED, ARCHIVED, DRAFT));

    public boolean canTransitionTo(PublicationStatus target) {
        return this == target || ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
