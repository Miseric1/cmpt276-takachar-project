package com.example.demo.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PublicationStatusTest {

    @ParameterizedTest
    @EnumSource(PublicationStatus.class)
    void everyStatusCanTransitionToItself(PublicationStatus status) {
        assertThat(status.canTransitionTo(status)).isTrue();
    }

    @Test
    void onlyPublishedIsPubliclyVisible() {
        for (PublicationStatus status : PublicationStatus.values()) {
            if (status == PublicationStatus.PUBLISHED) {
                assertThat(status.isPubliclyVisible()).isTrue();
            } else {
                assertThat(status.isPubliclyVisible()).isFalse();
            }
        }
    }

    @Test
    void draftCanMoveToReviewPublishedArchivedOrHidden() {
        assertThat(PublicationStatus.DRAFT.canTransitionTo(PublicationStatus.PENDING_REVIEW)).isTrue();
        assertThat(PublicationStatus.DRAFT.canTransitionTo(PublicationStatus.PUBLISHED)).isTrue();
        assertThat(PublicationStatus.DRAFT.canTransitionTo(PublicationStatus.ARCHIVED)).isTrue();
        assertThat(PublicationStatus.DRAFT.canTransitionTo(PublicationStatus.HIDDEN)).isTrue();
        assertThat(PublicationStatus.DRAFT.canTransitionTo(PublicationStatus.EXPIRED)).isFalse();
    }

    @Test
    void publishedCanMoveToHiddenArchivedExpiredOrBackToDraft() {
        assertThat(PublicationStatus.PUBLISHED.canTransitionTo(PublicationStatus.HIDDEN)).isTrue();
        assertThat(PublicationStatus.PUBLISHED.canTransitionTo(PublicationStatus.ARCHIVED)).isTrue();
        assertThat(PublicationStatus.PUBLISHED.canTransitionTo(PublicationStatus.EXPIRED)).isTrue();
        assertThat(PublicationStatus.PUBLISHED.canTransitionTo(PublicationStatus.DRAFT)).isTrue();
        assertThat(PublicationStatus.PUBLISHED.canTransitionTo(PublicationStatus.PENDING_REVIEW)).isFalse();
    }

    @Test
    void archivedCannotJumpDirectlyToHiddenOrExpired() {
        assertThat(PublicationStatus.ARCHIVED.canTransitionTo(PublicationStatus.HIDDEN)).isFalse();
        assertThat(PublicationStatus.ARCHIVED.canTransitionTo(PublicationStatus.EXPIRED)).isFalse();
        assertThat(PublicationStatus.ARCHIVED.canTransitionTo(PublicationStatus.DRAFT)).isTrue();
        assertThat(PublicationStatus.ARCHIVED.canTransitionTo(PublicationStatus.PUBLISHED)).isTrue();
    }

    @Test
    void expiredCanOnlyReturnToPublishedArchivedOrDraft() {
        assertThat(PublicationStatus.EXPIRED.canTransitionTo(PublicationStatus.PUBLISHED)).isTrue();
        assertThat(PublicationStatus.EXPIRED.canTransitionTo(PublicationStatus.ARCHIVED)).isTrue();
        assertThat(PublicationStatus.EXPIRED.canTransitionTo(PublicationStatus.DRAFT)).isTrue();
        assertThat(PublicationStatus.EXPIRED.canTransitionTo(PublicationStatus.HIDDEN)).isFalse();
        assertThat(PublicationStatus.EXPIRED.canTransitionTo(PublicationStatus.PENDING_REVIEW)).isFalse();
    }
}
