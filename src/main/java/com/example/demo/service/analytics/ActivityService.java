package com.example.demo.service.analytics;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.dashboard.ActivityDto;
import com.example.demo.model.Feedback;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.repository.KnowledgeArticleRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the recent-activity feed by merging the newest events across feedback
 * and Knowledge articles into a single time-ordered stream. Each source is
 * bounded (top 20) before merging, so the feed stays cheap as data grows.
 */
@Service
public class ActivityService {

    private final FeedbackRepository feedbackRepository;
    private final KnowledgeArticleRepository articleRepository;

    public ActivityService(FeedbackRepository feedbackRepository,
                           KnowledgeArticleRepository articleRepository) {
        this.feedbackRepository = feedbackRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityDto> getRecentActivity(Pageable pageable) {
        List<ActivityDto> merged = collectRecent();
        merged.sort(Comparator.comparing(ActivityDto::timestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        int from = (int) Math.min((long) pageable.getPageNumber() * pageable.getPageSize(), merged.size());
        int to = Math.min(from + pageable.getPageSize(), merged.size());
        List<ActivityDto> pageContent = merged.subList(from, to);

        int totalPages = (int) Math.ceil((double) merged.size() / pageable.getPageSize());
        return new PageResponse<>(pageContent, pageable.getPageNumber(), pageable.getPageSize(),
                merged.size(), totalPages,
                to < merged.size(), from > 0);
    }

    /** The newest entries, unpaginated, for embedding in the dashboard summary. */
    @Transactional(readOnly = true)
    public List<ActivityDto> getRecent(int limit) {
        List<ActivityDto> merged = collectRecent();
        merged.sort(Comparator.comparing(ActivityDto::timestamp, Comparator.nullsLast(Comparator.reverseOrder())));
        return merged.stream().limit(limit).toList();
    }

    private List<ActivityDto> collectRecent() {
        List<ActivityDto> events = new ArrayList<>();

        for (Feedback f : feedbackRepository.findTop20ByOrderByCreatedAtDesc()) {
            events.add(new ActivityDto("FEEDBACK_SUBMITTED",
                    "New feedback submitted" + (f.getCategory() != null ? " (" + f.getCategory() + ")" : ""),
                    f.getId(), f.getCreatedBy(), f.getCreatedAt()));
        }

        for (KnowledgeArticle a : articleRepository.findTop20ByOrderByUpdatedAtDesc()) {
            boolean created = isSameInstant(a.getCreatedAt(), a.getUpdatedAt());
            events.add(new ActivityDto(created ? "ARTICLE_CREATED" : "ARTICLE_UPDATED",
                    "Article \"" + truncate(a.getTitle()) + "\" " + (created ? "created" : "updated"),
                    a.getId(), a.getLastModifiedBy(), a.getUpdatedAt()));
        }

        return events;
    }

    private boolean isSameInstant(LocalDateTime a, LocalDateTime b) {
        return a != null && a.equals(b);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 60 ? text : text.substring(0, 57) + "...";
    }
}
