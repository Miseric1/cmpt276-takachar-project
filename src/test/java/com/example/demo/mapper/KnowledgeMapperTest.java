package com.example.demo.mapper;

import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.model.Category;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;
import com.example.demo.model.Tag;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeMapperTest {

    private KnowledgeArticle article() {
        Category category = new Category("Troubleshooting");
        category.setId(1L);

        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(10L);
        article.setTitle("Setup guide");
        article.setSummary("Short summary");
        article.setBody("Body text");
        article.setCategory(category);
        article.setStatus(PublicationStatus.PUBLISHED);
        article.setVersion(3);
        article.setEstimatedReadingTimeMinutes(4);
        article.setViewCount(100);
        article.setHelpfulCount(9);
        article.setNotHelpfulCount(1);
        article.setAuthor("author@test.com");
        article.setLastModifiedBy("editor@test.com");

        Set<Tag> tags = new LinkedHashSet<>();
        tags.add(new Tag("zeta"));
        tags.add(new Tag("alpha"));
        article.setTags(tags);

        Set<String> contributors = new LinkedHashSet<>();
        contributors.add("zoe@test.com");
        contributors.add("alan@test.com");
        article.setContributors(contributors);

        KnowledgeArticle related = new KnowledgeArticle();
        related.setId(20L);
        related.setTitle("Related guide");
        Set<KnowledgeArticle> relatedSet = new LinkedHashSet<>();
        relatedSet.add(related);
        article.setRelatedArticles(relatedSet);

        return article;
    }

    @Test
    void toResponseMapsAllFields() {
        KnowledgeArticle article = article();

        KnowledgeResponse response = KnowledgeMapper.toResponse(article);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Setup guide");
        assertThat(response.summary()).isEqualTo("Short summary");
        assertThat(response.body()).isEqualTo("Body text");
        assertThat(response.category().name()).isEqualTo("Troubleshooting");
        assertThat(response.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(response.version()).isEqualTo(3);
        assertThat(response.estimatedReadingTimeMinutes()).isEqualTo(4);
        assertThat(response.viewCount()).isEqualTo(100);
        assertThat(response.helpfulCount()).isEqualTo(9);
        assertThat(response.notHelpfulCount()).isEqualTo(1);
        assertThat(response.author()).isEqualTo("author@test.com");
        assertThat(response.lastModifiedBy()).isEqualTo("editor@test.com");
        assertThat(response.relatedArticles()).hasSize(1);
        assertThat(response.relatedArticles().get(0).id()).isEqualTo(20L);
        assertThat(response.relatedArticles().get(0).title()).isEqualTo("Related guide");
    }

    @Test
    void toResponseSortsTagsAlphabetically() {
        KnowledgeResponse response = KnowledgeMapper.toResponse(article());

        assertThat(response.tags()).containsExactly("alpha", "zeta");
    }

    @Test
    void toResponseSortsContributorsAlphabetically() {
        KnowledgeResponse response = KnowledgeMapper.toResponse(article());

        assertThat(response.contributors()).containsExactly("alan@test.com", "zoe@test.com");
    }

    @Test
    void toSummaryOmitsBodyAndRelatedArticles() {
        KnowledgeSummary summary = KnowledgeMapper.toSummary(article());

        assertThat(summary.id()).isEqualTo(10L);
        assertThat(summary.title()).isEqualTo("Setup guide");
        assertThat(summary.summary()).isEqualTo("Short summary");
        assertThat(summary.category().name()).isEqualTo("Troubleshooting");
        assertThat(summary.tags()).containsExactly("alpha", "zeta");
        assertThat(summary.status()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(summary.estimatedReadingTimeMinutes()).isEqualTo(4);
        assertThat(summary.viewCount()).isEqualTo(100);
        assertThat(summary.helpfulCount()).isEqualTo(9);
    }

    @Test
    void toResponseHandlesEmptyTagsRelatedArticlesAndContributors() {
        KnowledgeArticle article = article();
        article.setTags(new LinkedHashSet<>());
        article.setRelatedArticles(new LinkedHashSet<>());
        article.setContributors(new LinkedHashSet<>());

        KnowledgeResponse response = KnowledgeMapper.toResponse(article);

        assertThat(response.tags()).isEmpty();
        assertThat(response.relatedArticles()).isEmpty();
        assertThat(response.contributors()).isEmpty();
    }
}
