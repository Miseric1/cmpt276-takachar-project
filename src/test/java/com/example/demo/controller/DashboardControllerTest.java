package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.dashboard.ActivityDto;
import com.example.demo.dto.dashboard.DashboardOverviewDto;
import com.example.demo.dto.dashboard.DashboardSummaryDto;
import com.example.demo.dto.dashboard.FeedbackStatisticsDto;
import com.example.demo.dto.dashboard.KnowledgeStatisticsDto;
import com.example.demo.dto.dashboard.TicketStatisticsDto;
import com.example.demo.service.DashboardService;
import com.example.demo.service.analytics.ActivityService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private ActivityService activityService;

    private DashboardOverviewDto overview() {
        return new DashboardOverviewDto(0, 0, 0, 5, 2, 10, 8, 100, 3, 1);
    }

    @Test
    void getDashboardReturnsFullSummary() throws Exception {
        DashboardSummaryDto summary = new DashboardSummaryDto(
                overview(), TicketStatisticsDto.empty(),
                new FeedbackStatisticsDto(5, 2, 3, Map.of(), Map.of(), 1, 2, List.of()),
                new KnowledgeStatisticsDto(10, 8, 1, 1, Map.of(), 100, List.of(), List.of()),
                List.of());
        when(dashboardService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.totalArticles").value(10))
                .andExpect(jsonPath("$.feedback.total").value(5));
    }

    @Test
    void getOverviewReturnsCards() throws Exception {
        when(dashboardService.getOverview()).thenReturn(overview());

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedArticles").value(8))
                .andExpect(jsonPath("$.totalCustomers").value(3));
    }

    @Test
    void getTicketStatisticsReturnsZeroedPlaceholder() throws Exception {
        when(dashboardService.getTicketStatistics()).thenReturn(TicketStatisticsDto.empty());

        mockMvc.perform(get("/api/dashboard/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void getFeedbackStatisticsReturnsData() throws Exception {
        when(dashboardService.getFeedbackStatistics())
                .thenReturn(new FeedbackStatisticsDto(5, 2, 3, Map.of("OPEN", 2L), Map.of(), 1, 2, List.of()));

        mockMvc.perform(get("/api/dashboard/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(2));
    }

    @Test
    void getKnowledgeStatisticsReturnsData() throws Exception {
        when(dashboardService.getKnowledgeStatistics())
                .thenReturn(new KnowledgeStatisticsDto(10, 8, 1, 1, Map.of(), 100, List.of(), List.of()));

        mockMvc.perform(get("/api/dashboard/knowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalViews").value(100));
    }

    @Test
    void getRecentActivityReturnsPagedFeed() throws Exception {
        ActivityDto event = new ActivityDto("ARTICLE_CREATED", "Article \"Setup guide\" created", 1L, "admin@test.com", null);
        PageResponse<ActivityDto> page = new PageResponse<>(List.of(event), 0, 20, 1, 1, false, false);
        when(activityService.getRecentActivity(any())).thenReturn(page);

        mockMvc.perform(get("/api/dashboard/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("ARTICLE_CREATED"));
    }
}
