package com.example.demo.service;

import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.DashboardSummaryDto;
import com.example.demo.dto.dashboard.FeedbackStatisticsDto;
import com.example.demo.dto.dashboard.KnowledgeStatisticsDto;
import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.model.Feedback;
import com.example.demo.model.PublicationStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the dashboard aggregates real content counts and that the
 * not-yet-implemented ticket analytics degrade gracefully to zero.
 */
@SpringBootTest
@Transactional
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private FeedbackService feedbackService;

    @Test
    void overviewReflectsPublishedContent() {
        long before = dashboardService.getOverview().publishedArticles();

        knowledgeService.create(new KnowledgeRequest("Dashboard sample article", "A summary.",
                "Some body content.", "General", Set.of("sample"), null, "admin@test.com",
                PublicationStatus.PUBLISHED), "admin@test.com");

        DashboardOverviewDto overview = dashboardService.getOverview();
        assertThat(overview.publishedArticles()).isEqualTo(before + 1);
        assertThat(overview.totalStaff()).isGreaterThanOrEqualTo(1); // seeded admin
    }

    @Test
    void summaryIncludesEmptyTicketStatistics() {
        DashboardSummaryDto summary = dashboardService.getSummary();

        assertThat(summary.tickets()).isNotNull();
        assertThat(summary.tickets().total()).isZero();
        assertThat(summary.feedback()).isNotNull();
        assertThat(summary.knowledge()).isNotNull();
        assertThat(summary.recentActivity()).isNotNull();
    }

    @Test
    void feedbackStatisticsReflectStatusAndCategoryBreakdown() {
        long openBefore = dashboardService.getFeedbackStatistics().open();

        Feedback resolved = new Feedback("Bug", "ProjectA", "Acct1", "Broken button", "user1");
        resolved.setStatus("RESOLVED");
        feedbackService.createFeedback(resolved);
        feedbackService.createFeedback(new Feedback("Bug", "ProjectA", "Acct2", "Slow load", "user2"));

        FeedbackStatisticsDto stats = dashboardService.getFeedbackStatistics();

        assertThat(stats.open()).isEqualTo(openBefore + 1);
        assertThat(stats.byStatus().getOrDefault("RESOLVED", 0L)).isGreaterThanOrEqualTo(1);
        assertThat(stats.byCategory().getOrDefault("Bug", 0L)).isGreaterThanOrEqualTo(2);
        assertThat(stats.charts()).isNotEmpty();
    }

    @Test
    void knowledgeStatisticsRankMostViewedArticles() {
        KnowledgeResponse popular = knowledgeService.create(new KnowledgeRequest("Most viewed guide", "Summary.",
                "Body content.", "General", Set.of(), null, "author@test.com", PublicationStatus.PUBLISHED),
                "author@test.com");
        knowledgeService.getPublishedByIdAndCountView(popular.id());
        knowledgeService.getPublishedByIdAndCountView(popular.id());
        knowledgeService.getPublishedByIdAndCountView(popular.id());

        knowledgeService.create(new KnowledgeRequest("Rarely viewed guide", "Summary.",
                "Other body.", "General", Set.of(), null, "author@test.com", PublicationStatus.PUBLISHED),
                "author@test.com");

        KnowledgeStatisticsDto stats = dashboardService.getKnowledgeStatistics();

        assertThat(stats.published()).isGreaterThanOrEqualTo(2);
        assertThat(stats.totalViews()).isGreaterThanOrEqualTo(3);
        assertThat(stats.mostViewed()).isNotEmpty();
        assertThat(stats.mostViewed().get(0).id()).isEqualTo(popular.id());
    }

    @Test
    void overviewCountsMatchKnowledgeBaseTotals() {
        knowledgeService.create(new KnowledgeRequest("Overview guide", "Summary.",
                "Body content.", "General", Set.of(), null, "author@test.com", PublicationStatus.PUBLISHED),
                "author@test.com");

        DashboardOverviewDto overview = dashboardService.getOverview();

        assertThat(overview.publishedArticles()).isGreaterThanOrEqualTo(1);
        assertThat(overview.totalArticles()).isGreaterThanOrEqualTo(overview.publishedArticles());
    }
}
