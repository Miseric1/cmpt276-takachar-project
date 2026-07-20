package com.example.demo.service;

import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.dto.knowledge.KnowledgeVersionDto;
import com.example.demo.model.PublicationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
}
