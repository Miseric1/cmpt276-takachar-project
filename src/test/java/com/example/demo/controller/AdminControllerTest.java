package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.dashboard.ThemeSummary;
import com.example.demo.dto.dashboard.TicketStatisticsDto;
import com.example.demo.dto.ticket.TicketSummary;
import com.example.demo.service.DashboardService;
import com.example.demo.service.FeedbackService;
import com.example.demo.service.TicketService;
import com.example.demo.service.analytics.FeedbackThemeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@WithMockUser(username = "admin@test.com", roles = "ADMIN")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private FeedbackThemeService feedbackThemeService;

    private final List<ThemeSummary> themes = List.of(
            new ThemeSummary("overheating", 3,
                    Map.of("PRODUCT", 3L), Map.of("NEGATIVE", 3L))
    );

    @BeforeEach
    void setUp() {
        when(feedbackService.getAllFeedback()).thenReturn(List.of());

        when(dashboardService.getTicketStatistics())
                .thenReturn(TicketStatisticsDto.empty());

        PageResponse<TicketSummary> emptyPage =
                new PageResponse<>(List.of(), 0, 50, 0, 0, false, false);
        when(ticketService.search(
                any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), anyBoolean()))
                .thenReturn(emptyPage);

        when(feedbackThemeService.extractThemes(any(), anyInt()))
                .thenReturn(themes);
    }

    @Test
    void adminHomeExposesRecurringThemes() throws Exception {
        mockMvc.perform(get("/admin/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("themes", themes));

        verify(feedbackThemeService)
                .extractThemes(Mockito.anyList(), eq(5));
    }

    @Test
    void feedbackTrackerExposesRecurringThemes() throws Exception {
        mockMvc.perform(get("/admin/feedback"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-feedback"))
                .andExpect(model().attribute("themes", themes));

        verify(feedbackThemeService)
                .extractThemes(Mockito.anyList(), eq(10));
    }
}
