package com.example.demo.service;

import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.DashboardSummaryDto;
import com.example.demo.dto.faq.FaqRequest;
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
    private FaqService faqService;

    @Test
    void overviewReflectsPublishedContent() {
        long before = dashboardService.getOverview().publishedFaqs();

        faqService.create(new FaqRequest("Dashboard sample question?", "An answer.", "General",
                Set.of("sample"), 0, PublicationStatus.PUBLISHED), "admin@test.com");

        DashboardOverviewDto overview = dashboardService.getOverview();
        assertThat(overview.publishedFaqs()).isEqualTo(before + 1);
        assertThat(overview.totalStaff()).isGreaterThanOrEqualTo(1); // seeded admin
    }

    @Test
    void summaryIncludesEmptyTicketStatistics() {
        DashboardSummaryDto summary = dashboardService.getSummary();

        assertThat(summary.tickets()).isNotNull();
        assertThat(summary.tickets().total()).isZero();
        assertThat(summary.feedback()).isNotNull();
        assertThat(summary.faq()).isNotNull();
        assertThat(summary.knowledge()).isNotNull();
        assertThat(summary.recentActivity()).isNotNull();
    }
}
