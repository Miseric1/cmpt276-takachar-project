package com.example.demo.service;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.dto.knowledge.KnowledgeVersionDto;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
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
 * End-to-end tests for the Knowledge Base module: reading-time estimation,
 * related-article links, versioning, and contributor tracking.
 */
@SpringBootTest
@Transactional
class KnowledgeServiceTest {

    @Autowired
    private KnowledgeService knowledgeService;

    private KnowledgeRequest request(String title, String body, Set<Long> related, PublicationStatus status) {
        return new KnowledgeRequest(title, "A short summary.", body, "Troubleshooting",
                Set.of("network", "setup"), related, "author@test.com", status);
    }

    @Test
    void createsArticleAndEstimatesReadingTime() {
        String body = "word ".repeat(400).trim(); // 400 words -> ~2 minutes at 200 wpm
        KnowledgeResponse created = knowledgeService.create(
                request("Setting up your device", body, null, PublicationStatus.PUBLISHED), "author@test.com");

        assertThat(created.id()).isNotNull();
        assertThat(created.estimatedReadingTimeMinutes()).isEqualTo(2);
        assertThat(created.tags()).containsExactlyInAnyOrder("network", "setup");
        assertThat(created.contributors()).contains("author@test.com");
        assertThat(created.publishedAt()).isNotNull();
    }

    @Test
    void linksRelatedArticles() {
        KnowledgeResponse first = knowledgeService.create(
                request("First guide", "Some body content here.", null, PublicationStatus.DRAFT), "author@test.com");
        KnowledgeResponse second = knowledgeService.create(
                request("Second guide", "Other body content here.", Set.of(first.id()), PublicationStatus.DRAFT),
                "author@test.com");

        assertThat(second.relatedArticles()).hasSize(1);
        assertThat(second.relatedArticles().get(0).id()).isEqualTo(first.id());
    }

    @Test
    void updateCreatesVersionAndAddsContributor() {
        KnowledgeResponse created = knowledgeService.create(
                request("Editable guide", "Original body.", null, PublicationStatus.DRAFT), "author@test.com");

        KnowledgeRequest edit = new KnowledgeRequest("Editable guide", "New summary.", "Rewritten body.",
                "Troubleshooting", Set.of("network"), null, "author@test.com", null);
        KnowledgeResponse updated = knowledgeService.update(created.id(), edit, "editor@test.com");

        assertThat(updated.version()).isEqualTo(2);
        assertThat(updated.contributors()).contains("author@test.com", "editor@test.com");

        List<KnowledgeVersionDto> versions = knowledgeService.getVersions(created.id());
        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).body()).isEqualTo("Original body.");
    }

    @Test
    void createThrowsOnDuplicateTitle() {
        knowledgeService.create(
                request("Unique title", "Body one.", null, PublicationStatus.DRAFT), "author@test.com");

        assertThatThrownBy(() -> knowledgeService.create(
                request("Unique title", "Body two.", null, PublicationStatus.DRAFT), "author@test.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateThrowsWhenTitleTakenByAnotherArticle() {
        knowledgeService.create(
                request("First title", "Body.", null, PublicationStatus.DRAFT), "author@test.com");
        KnowledgeResponse second = knowledgeService.create(
                request("Second title", "Body.", null, PublicationStatus.DRAFT), "author@test.com");

        KnowledgeRequest edit = new KnowledgeRequest("First title", "Summary.", "Body.",
                "Troubleshooting", Set.of(), null, "author@test.com", null);

        assertThatThrownBy(() -> knowledgeService.update(second.id(), edit, "editor@test.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void changeStatusRejectsIllegalTransition() {
        KnowledgeResponse created = knowledgeService.create(
                request("Archived guide", "Body.", null, PublicationStatus.ARCHIVED), "author@test.com");

        assertThatThrownBy(() -> knowledgeService.changeStatus(created.id(), PublicationStatus.HIDDEN, "admin@test.com"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void changeStatusAllowsPublishingAndStampsPublishedAt() {
        KnowledgeResponse created = knowledgeService.create(
                request("Draft guide", "Body.", null, PublicationStatus.DRAFT), "author@test.com");
        assertThat(created.publishedAt()).isNull();

        KnowledgeResponse published = knowledgeService.changeStatus(created.id(), PublicationStatus.PUBLISHED, "admin@test.com");

        assertThat(published.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(published.publishedAt()).isNotNull();
    }

    @Test
    void deleteClearsItsOwnRelatedArticleLinksAndRemovesTheArticle() {
        KnowledgeResponse related = knowledgeService.create(
                request("Related guide", "Body.", null, PublicationStatus.DRAFT), "author@test.com");
        KnowledgeResponse target = knowledgeService.create(
                request("Guide to delete", "Body.", Set.of(related.id()), PublicationStatus.DRAFT), "author@test.com");
        assertThat(target.relatedArticles()).hasSize(1);

        knowledgeService.delete(target.id());

        assertThatThrownBy(() -> knowledgeService.getById(target.id()))
                .isInstanceOf(ResourceNotFoundException.class);
        // The article it used to link to is unaffected by the deletion.
        assertThat(knowledgeService.getById(related.id())).isNotNull();
    }

    @Test
    void deleteRemovesIncomingReferencesFromOtherArticles() {
        KnowledgeResponse a = knowledgeService.create(
                request("Article A", "Body.", null, PublicationStatus.DRAFT), "author@test.com");
        KnowledgeResponse b = knowledgeService.create(
                request("Article B", "Body.", Set.of(a.id()), PublicationStatus.DRAFT), "author@test.com");
        assertThat(b.relatedArticles()).hasSize(1);

        knowledgeService.delete(a.id());

        // B no longer points at the deleted article, and loading it doesn't error.
        KnowledgeResponse reloadedB = knowledgeService.getById(b.id());
        assertThat(reloadedB.relatedArticles()).isEmpty();
    }

    @Test
    void deleteThrowsWhenArticleMissing() {
        assertThatThrownBy(() -> knowledgeService.delete(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markHelpfulAndNotHelpfulIncrementCounters() {
        KnowledgeResponse created = knowledgeService.create(
                request("Feedback guide", "Body.", null, PublicationStatus.PUBLISHED), "author@test.com");

        knowledgeService.markHelpful(created.id());
        knowledgeService.markHelpful(created.id());
        knowledgeService.markNotHelpful(created.id());

        KnowledgeResponse reloaded = knowledgeService.getById(created.id());
        assertThat(reloaded.helpfulCount()).isEqualTo(2);
        assertThat(reloaded.notHelpfulCount()).isEqualTo(1);
    }

    @Test
    void markHelpfulThrowsWhenArticleMissing() {
        assertThatThrownBy(() -> knowledgeService.markHelpful(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublishedByIdIncrementsViewCount() {
        KnowledgeResponse created = knowledgeService.create(
                request("Viewed guide", "Body.", null, PublicationStatus.PUBLISHED), "author@test.com");
        assertThat(created.viewCount()).isEqualTo(0);

        knowledgeService.getPublishedByIdAndCountView(created.id());
        KnowledgeResponse afterSecondView = knowledgeService.getPublishedByIdAndCountView(created.id());

        assertThat(afterSecondView.viewCount()).isEqualTo(2);
    }

    @Test
    void getPublishedByIdRejectsNonPublishedArticle() {
        KnowledgeResponse draft = knowledgeService.create(
                request("Hidden from public", "Body.", null, PublicationStatus.DRAFT), "author@test.com");

        assertThatThrownBy(() -> knowledgeService.getPublishedByIdAndCountView(draft.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdThrowsWhenArticleMissing() {
        assertThatThrownBy(() -> knowledgeService.getById(99999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchFiltersByKeywordCategoryAndStatus() {
        knowledgeService.create(new KnowledgeRequest("Printer jam guide", "Summary.", "How to fix a printer jam.",
                "Hardware", Set.of("printer"), null, "author@test.com", PublicationStatus.PUBLISHED), "author@test.com");
        knowledgeService.create(new KnowledgeRequest("Billing FAQ", "Summary.", "Billing questions answered.",
                "Billing", Set.of("billing"), null, "author@test.com", PublicationStatus.DRAFT), "author@test.com");

        PageResponse<KnowledgeSummary> byKeyword = knowledgeService.search(
                "printer", null, null, null, null, PageRequest.of(0, 10));
        assertThat(byKeyword.getContent()).hasSize(1);
        assertThat(byKeyword.getContent().get(0).title()).isEqualTo("Printer jam guide");

        PageResponse<KnowledgeSummary> byCategory = knowledgeService.search(
                null, "Billing", null, null, null, PageRequest.of(0, 10));
        assertThat(byCategory.getContent()).hasSize(1);
        assertThat(byCategory.getContent().get(0).title()).isEqualTo("Billing FAQ");

        PageResponse<KnowledgeSummary> byStatus = knowledgeService.search(
                null, null, null, null, PublicationStatus.PUBLISHED, PageRequest.of(0, 10));
        assertThat(byStatus.getContent()).extracting(KnowledgeSummary::title).containsExactly("Printer jam guide");
    }

    @Test
    void searchPublishedOnlyReturnsPublishedArticles() {
        knowledgeService.create(
                request("Published guide", "Body.", null, PublicationStatus.PUBLISHED), "author@test.com");
        knowledgeService.create(
                request("Draft guide", "Body.", null, PublicationStatus.DRAFT), "author@test.com");

        PageResponse<KnowledgeSummary> results = knowledgeService.searchPublished(
                null, null, null, PageRequest.of(0, 10));

        assertThat(results.getContent()).extracting(KnowledgeSummary::title).containsExactly("Published guide");
    }

    @Test
    void getVersionReturnsSpecificSnapshot() {
        KnowledgeResponse created = knowledgeService.create(
                request("Versioned guide", "Version one body.", null, PublicationStatus.DRAFT), "author@test.com");
        KnowledgeRequest edit = new KnowledgeRequest("Versioned guide", "Summary.", "Version two body.",
                "Troubleshooting", Set.of(), null, "author@test.com", null);
        knowledgeService.update(created.id(), edit, "editor@test.com");

        KnowledgeVersionDto version1 = knowledgeService.getVersion(created.id(), 1);

        assertThat(version1.body()).isEqualTo("Version one body.");
    }

    @Test
    void getVersionThrowsWhenVersionMissing() {
        KnowledgeResponse created = knowledgeService.create(
                request("Single version guide", "Body.", null, PublicationStatus.DRAFT), "author@test.com");

        assertThatThrownBy(() -> knowledgeService.getVersion(created.id(), 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
