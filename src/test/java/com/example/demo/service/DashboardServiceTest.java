package com.example.demo.service;

import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.DashboardSummaryDto;
import com.example.demo.dto.knowledge.KnowledgeRequest;
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
}
