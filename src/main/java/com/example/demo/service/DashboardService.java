package com.example.demo.service;

import com.example.demo.UserRepository;
import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.DashboardSummaryDto;
import com.example.demo.dto.dashboard.FaqStatisticsDto;
import com.example.demo.dto.dashboard.FeedbackStatisticsDto;
import com.example.demo.dto.dashboard.KnowledgeStatisticsDto;
import com.example.demo.dto.dashboard.TicketStatisticsDto;
import com.example.demo.service.analytics.ActivityService;
import com.example.demo.service.analytics.FaqAnalyticsService;
import com.example.demo.service.analytics.FeedbackAnalyticsService;
import com.example.demo.service.analytics.KnowledgeAnalyticsService;
import com.example.demo.service.analytics.TicketAnalyticsProvider;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates the per-domain analytics services into the dashboard payloads.
 * It owns no analytics logic of its own -- it composes the specialised services
 * -- which keeps each analytic reusable for future reports and keeps this class
 * a thin orchestration layer.
 */
@Service
public class DashboardService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CUSTOMER = "CUSTOMER";

    private final FeedbackAnalyticsService feedbackAnalytics;
    private final FaqAnalyticsService faqAnalytics;
    private final KnowledgeAnalyticsService knowledgeAnalytics;
    private final TicketAnalyticsProvider ticketAnalytics;
    private final ActivityService activityService;
    private final UserRepository userRepository;

    public DashboardService(FeedbackAnalyticsService feedbackAnalytics,
                            FaqAnalyticsService faqAnalytics,
                            KnowledgeAnalyticsService knowledgeAnalytics,
                            TicketAnalyticsProvider ticketAnalytics,
                            ActivityService activityService,
                            UserRepository userRepository) {
        this.feedbackAnalytics = feedbackAnalytics;
        this.faqAnalytics = faqAnalytics;
        this.knowledgeAnalytics = knowledgeAnalytics;
        this.ticketAnalytics = ticketAnalytics;
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    /** Lightweight top-of-page cards. Cheap counts only. */
    @Transactional(readOnly = true)
    public DashboardOverviewDto getOverview() {
        TicketStatisticsDto tickets = ticketAnalytics.getStatistics();
        return new DashboardOverviewDto(
                tickets.total(),
                tickets.open(),
                tickets.overdue(),
                feedbackAnalytics.total(),
                feedbackAnalytics.open(),
                faqAnalytics.total(),
                faqAnalytics.published(),
                knowledgeAnalytics.total(),
                knowledgeAnalytics.published(),
                knowledgeAnalytics.totalViews(),
                userRepository.countByRoleIgnoreCase(ROLE_CUSTOMER),
                userRepository.countByRoleIgnoreCase(ROLE_ADMIN));
    }

    /** The full dashboard: overview plus every analytics block and the feed. */
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        return new DashboardSummaryDto(
                getOverview(),
                ticketAnalytics.getStatistics(),
                feedbackAnalytics.getStatistics(),
                faqAnalytics.getStatistics(),
                knowledgeAnalytics.getStatistics(),
                activityService.getRecent(15));
    }

    @Transactional(readOnly = true)
    public TicketStatisticsDto getTicketStatistics() {
        return ticketAnalytics.getStatistics();
    }

    @Transactional(readOnly = true)
    public FeedbackStatisticsDto getFeedbackStatistics() {
        return feedbackAnalytics.getStatistics();
    }

    @Transactional(readOnly = true)
    public FaqStatisticsDto getFaqStatistics() {
        return faqAnalytics.getStatistics();
    }

    @Transactional(readOnly = true)
    public KnowledgeStatisticsDto getKnowledgeStatistics() {
        return knowledgeAnalytics.getStatistics();
    }
}
