package com.example.demo.dto.dashboard;

import java.util.List;

/**
 * The full dashboard payload returned by {@code GET /api/dashboard}: the
 * overview cards plus every analytics block and the recent-activity feed, so a
 * dashboard page can render from a single request.
 */
public record DashboardSummaryDto(
        DashboardOverviewDto overview,
        TicketStatisticsDto tickets,
        FeedbackStatisticsDto feedback,
        KnowledgeStatisticsDto knowledge,
        List<ActivityDto> recentActivity) {
}
