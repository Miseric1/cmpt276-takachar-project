package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.faq.FaqRequest;
import com.example.demo.dto.faq.FaqResponse;
import com.example.demo.dto.faq.FaqSummary;
import com.example.demo.dto.faq.FaqVersionDto;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.model.PublicationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end tests for the FAQ module exercising the real repositories,
 * category/tag resolution, versioning, publication workflow, and search.
 * Each test runs in a transaction that rolls back, keeping them isolated.
 */
@SpringBootTest
@Transactional
class FaqServiceTest {

    @Autowired
    private FaqService faqService;

    private FaqRequest sampleRequest(String question, PublicationStatus status) {
        return new FaqRequest(question, "You reset it from the account page.", "Accounts",
                Set.of("password", "login"), 1, status);
    }

    @Test
    void createsFaqWithResolvedCategoryAndTags() {
        FaqResponse created = faqService.create(sampleRequest("How do I reset my password?", null), "admin@test.com");

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(created.version()).isEqualTo(1);
        assertThat(created.category().name()).isEqualTo("Accounts");
        assertThat(created.tags()).containsExactlyInAnyOrder("password", "login");
        assertThat(created.createdBy()).isEqualTo("admin@test.com");
    }

    @Test
    void rejectsDuplicateQuestion() {
        faqService.create(sampleRequest("Where is my invoice?", null), "admin@test.com");
        assertThatThrownBy(() -> faqService.create(sampleRequest("where is my invoice?", null), "admin@test.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateSnapshotsPreviousVersionAndBumpsVersion() {
        FaqResponse created = faqService.create(sampleRequest("Why won't my device connect?", null), "admin@test.com");

        FaqRequest edit = new FaqRequest("Why won't my device connect?", "Check the network settings.",
                "Technical Support", Set.of("network"), 2, null);
        FaqResponse updated = faqService.update(created.id(), edit, "editor@test.com");

        assertThat(updated.version()).isEqualTo(2);
        assertThat(updated.answer()).isEqualTo("Check the network settings.");
        assertThat(updated.lastModifiedBy()).isEqualTo("editor@test.com");

        List<FaqVersionDto> versions = faqService.getVersions(created.id());
        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).versionNumber()).isEqualTo(1);
        assertThat(versions.get(0).answer()).isEqualTo("You reset it from the account page.");
    }

    @Test
    void publishesAndExposesOnlyPublishedContentToPublicSearch() {
        faqService.create(sampleRequest("Published question about billing?", PublicationStatus.PUBLISHED), "admin@test.com");
        faqService.create(sampleRequest("Draft question about billing?", PublicationStatus.DRAFT), "admin@test.com");

        PageResponse<FaqSummary> published = faqService.searchPublished("billing", null, null, PageRequest.of(0, 10));
        assertThat(published.getContent()).hasSize(1);
        assertThat(published.getContent().get(0).status()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void rejectsIllegalStatusTransition() {
        FaqResponse created = faqService.create(sampleRequest("Archivable question?", PublicationStatus.ARCHIVED), "admin@test.com");
        // ARCHIVED -> EXPIRED is not an allowed transition.
        assertThatThrownBy(() -> faqService.changeStatus(created.id(), PublicationStatus.EXPIRED, "admin@test.com"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void marksHelpfulIncrementsCounter() {
        FaqResponse created = faqService.create(sampleRequest("Helpful question?", PublicationStatus.PUBLISHED), "admin@test.com");
        faqService.markHelpful(created.id());

        FaqResponse reloaded = faqService.getById(created.id());
        assertThat(reloaded.helpfulCount()).isEqualTo(1);
    }
}
