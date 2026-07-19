package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.KnowledgeStatisticsDto;
import com.example.demo.dto.dashboard.PopularContent;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.PublicationStatus;
import com.example.demo.repository.KnowledgeArticleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics over the Knowledge Base: status distribution, total views, and
 * most-viewed / recently-updated leaderboards.
 */
@Service
public class KnowledgeAnalyticsService {

    private final KnowledgeArticleRepository articleRepository;

    public KnowledgeAnalyticsService(KnowledgeArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public long total() {
        return articleRepository.count();
    }

    @Transactional(readOnly = true)
    public long published() {
        return articleRepository.countByStatus(PublicationStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public long totalViews() {
        return articleRepository.sumViewCount();
    }

    @Transactional(readOnly = true)
    public KnowledgeStatisticsDto getStatistics() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (PublicationStatus status : PublicationStatus.values()) {
            long count = articleRepository.countByStatus(status);
            if (count > 0) {
                byStatus.put(status.name(), count);
            }
        }

        long total = articleRepository.count();
        long published = byStatus.getOrDefault(PublicationStatus.PUBLISHED.name(), 0L);
        long draft = byStatus.getOrDefault(PublicationStatus.DRAFT.name(), 0L);
        long archived = byStatus.getOrDefault(PublicationStatus.ARCHIVED.name(), 0L);

        List<PopularContent> mostViewed = toPopular(
                articleRepository.findTop5ByStatusOrderByViewCountDesc(PublicationStatus.PUBLISHED));
        List<PopularContent> recentlyUpdated = toPopular(
                articleRepository.findTop5ByStatusOrderByUpdatedAtDesc(PublicationStatus.PUBLISHED));

        return new KnowledgeStatisticsDto(total, published, draft, archived, byStatus,
                articleRepository.sumViewCount(), mostViewed, recentlyUpdated);
    }

    private List<PopularContent> toPopular(List<KnowledgeArticle> articles) {
        return articles.stream()
                .map(a -> new PopularContent(a.getId(), a.getTitle(), a.getViewCount(), a.getHelpfulCount()))
                .toList();
    }
}
