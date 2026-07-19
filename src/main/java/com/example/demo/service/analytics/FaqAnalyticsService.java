package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.FaqStatisticsDto;
import com.example.demo.dto.dashboard.PopularContent;
import com.example.demo.model.Faq;
import com.example.demo.model.PublicationStatus;
import com.example.demo.repository.FaqRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics over the FAQ module: status distribution, total views, and
 * popularity leaderboards. Leaderboards consider only published FAQs, since
 * those are what customers actually see.
 */
@Service
public class FaqAnalyticsService {

    private final FaqRepository faqRepository;

    public FaqAnalyticsService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Transactional(readOnly = true)
    public long total() {
        return faqRepository.count();
    }

    @Transactional(readOnly = true)
    public long published() {
        return faqRepository.countByStatus(PublicationStatus.PUBLISHED);
    }

    @Transactional(readOnly = true)
    public FaqStatisticsDto getStatistics() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (PublicationStatus status : PublicationStatus.values()) {
            long count = faqRepository.countByStatus(status);
            if (count > 0) {
                byStatus.put(status.name(), count);
            }
        }

        long total = faqRepository.count();
        long published = byStatus.getOrDefault(PublicationStatus.PUBLISHED.name(), 0L);
        long draft = byStatus.getOrDefault(PublicationStatus.DRAFT.name(), 0L);
        long archived = byStatus.getOrDefault(PublicationStatus.ARCHIVED.name(), 0L);

        List<PopularContent> mostViewed = toPopular(
                faqRepository.findTop5ByStatusOrderByViewCountDesc(PublicationStatus.PUBLISHED));
        List<PopularContent> leastViewed = toPopular(
                faqRepository.findTop5ByStatusOrderByViewCountAsc(PublicationStatus.PUBLISHED));
        List<PopularContent> mostHelpful = toPopular(
                faqRepository.findTop5ByStatusOrderByHelpfulCountDesc(PublicationStatus.PUBLISHED));

        return new FaqStatisticsDto(total, published, draft, archived, byStatus,
                faqRepository.sumViewCount(), mostViewed, leastViewed, mostHelpful);
    }

    private List<PopularContent> toPopular(List<Faq> faqs) {
        return faqs.stream()
                .map(f -> new PopularContent(f.getId(), f.getQuestion(), f.getViewCount(), f.getHelpfulCount()))
                .toList();
    }
}
