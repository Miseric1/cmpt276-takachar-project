package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.dashboard.ActivityDto;
import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.DashboardSummaryDto;
import com.example.demo.dto.dashboard.FaqStatisticsDto;
import com.example.demo.dto.dashboard.FeedbackStatisticsDto;
import com.example.demo.dto.dashboard.KnowledgeStatisticsDto;
import com.example.demo.dto.dashboard.TicketStatisticsDto;
import com.example.demo.service.DashboardService;
import com.example.demo.service.analytics.ActivityService;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only analytics API for the staff dashboard. Every endpoint is staff-only
 * (enforced in the security configuration) and returns already-computed,
 * visualization-ready data so the frontend performs no aggregation itself.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ActivityService activityService;

    public DashboardController(DashboardService dashboardService, ActivityService activityService) {
        this.dashboardService = dashboardService;
        this.activityService = activityService;
    }

    /** The complete dashboard in one request. */
    @GetMapping
    public DashboardSummaryDto getDashboard() {
        return dashboardService.getSummary();
    }

    @GetMapping("/overview")
    public DashboardOverviewDto getOverview() {
        return dashboardService.getOverview();
    }

    @GetMapping("/tickets")
    public TicketStatisticsDto getTicketStatistics() {
        return dashboardService.getTicketStatistics();
    }

    @GetMapping("/feedback")
    public FeedbackStatisticsDto getFeedbackStatistics() {
        return dashboardService.getFeedbackStatistics();
    }

    @GetMapping("/faq")
    public FaqStatisticsDto getFaqStatistics() {
        return dashboardService.getFaqStatistics();
    }

    @GetMapping("/knowledge")
    public KnowledgeStatisticsDto getKnowledgeStatistics() {
        return dashboardService.getKnowledgeStatistics();
    }

    @GetMapping("/activity")
    public PageResponse<ActivityDto> getRecentActivity(
            @PageableDefault(size = 20) Pageable pageable) {
        return activityService.getRecentActivity(pageable);
    }
}
