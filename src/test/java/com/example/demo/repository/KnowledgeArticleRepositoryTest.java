package com.example.demo.repository;

import com.example.demo.model.Category;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class KnowledgeArticleRepositoryTest {

    @Autowired
    private KnowledgeArticleRepository articleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category sharedCategory;

    private Category generalCategory() {
        if (sharedCategory == null) {
            sharedCategory = new Category("General");
            entityManager.persist(sharedCategory);
        }
        return sharedCategory;
    }

    private KnowledgeArticle article(String title, PublicationStatus status) {
        Category category = generalCategory();

        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(title);
        article.setBody("Body text");
        article.setCategory(category);
        article.setStatus(status);
        return articleRepository.save(article);
    }

    @Test
    void findByTitleIgnoreCaseMatchesRegardlessOfCase() {
        article("Setup Guide", PublicationStatus.DRAFT);
        entityManager.flush();

        Optional<KnowledgeArticle> found = articleRepository.findByTitleIgnoreCase("setup guide");

        assertThat(found).isPresent();
    }

    @Test
    void existsByTitleIgnoreCaseIsTrueForMatchingTitle() {
        article("Setup Guide", PublicationStatus.DRAFT);
        entityManager.flush();

        assertThat(articleRepository.existsByTitleIgnoreCase("SETUP GUIDE")).isTrue();
        assertThat(articleRepository.existsByTitleIgnoreCase("Other guide")).isFalse();
    }

    @Test
    void countByStatusCountsOnlyMatchingArticles() {
        article("Published one", PublicationStatus.PUBLISHED);
        article("Published two", PublicationStatus.PUBLISHED);
        article("Draft one", PublicationStatus.DRAFT);
        entityManager.flush();

        assertThat(articleRepository.countByStatus(PublicationStatus.PUBLISHED)).isEqualTo(2);
        assertThat(articleRepository.countByStatus(PublicationStatus.DRAFT)).isEqualTo(1);
        assertThat(articleRepository.countByStatus(PublicationStatus.ARCHIVED)).isEqualTo(0);
    }

    @Test
    void findByStatusReturnsOnlyMatchingArticlesPaged() {
        article("Published one", PublicationStatus.PUBLISHED);
        article("Draft one", PublicationStatus.DRAFT);
        entityManager.flush();

        Page<KnowledgeArticle> page = articleRepository.findByStatus(PublicationStatus.PUBLISHED, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Published one");
    }

    @Test
    void findTop5ByStatusOrderByViewCountDescRanksByViews() {
        KnowledgeArticle low = article("Low views", PublicationStatus.PUBLISHED);
        low.setViewCount(1);
        KnowledgeArticle high = article("High views", PublicationStatus.PUBLISHED);
        high.setViewCount(50);
        articleRepository.saveAll(List.of(low, high));
        entityManager.flush();

        List<KnowledgeArticle> ranked = articleRepository.findTop5ByStatusOrderByViewCountDesc(PublicationStatus.PUBLISHED);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).getTitle()).isEqualTo("High views");
    }

    @Test
    void sumViewCountAggregatesAcrossAllArticles() {
        KnowledgeArticle a = article("A", PublicationStatus.PUBLISHED);
        a.setViewCount(10);
        KnowledgeArticle b = article("B", PublicationStatus.DRAFT);
        b.setViewCount(5);
        articleRepository.saveAll(List.of(a, b));
        entityManager.flush();

        assertThat(articleRepository.sumViewCount()).isEqualTo(15);
    }

    @Test
    void sumViewCountReturnsZeroWhenNoArticles() {
        assertThat(articleRepository.sumViewCount()).isEqualTo(0);
    }

    @Test
    void incrementViewCountAtomicallyBumpsCounter() {
        KnowledgeArticle saved = article("Setup Guide", PublicationStatus.PUBLISHED);
        entityManager.flush();

        articleRepository.incrementViewCount(saved.getId());
        articleRepository.incrementViewCount(saved.getId());

        KnowledgeArticle reloaded = articleRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getViewCount()).isEqualTo(2);
    }

    @Test
    void incrementHelpfulAndNotHelpfulCountsAreIndependent() {
        KnowledgeArticle saved = article("Setup Guide", PublicationStatus.PUBLISHED);
        entityManager.flush();

        articleRepository.incrementHelpfulCount(saved.getId());
        articleRepository.incrementNotHelpfulCount(saved.getId());
        articleRepository.incrementNotHelpfulCount(saved.getId());

        KnowledgeArticle reloaded = articleRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getHelpfulCount()).isEqualTo(1);
        assertThat(reloaded.getNotHelpfulCount()).isEqualTo(2);
    }

    @Test
    void findTop20ByOrderByUpdatedAtDescReturnsMostRecentFirst() throws InterruptedException {
        article("First", PublicationStatus.DRAFT);
        entityManager.flush();
        Thread.sleep(5);
        article("Second", PublicationStatus.DRAFT);
        entityManager.flush();

        List<KnowledgeArticle> recent = articleRepository.findTop20ByOrderByUpdatedAtDesc();

        assertThat(recent.get(0).getTitle()).isEqualTo("Second");
    }
}
