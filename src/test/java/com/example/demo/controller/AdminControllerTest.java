package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.account.CustomerAccountResponse;
import com.example.demo.dto.dashboard.ThemeSummary;
import com.example.demo.dto.dashboard.TicketStatisticsDto;
import com.example.demo.dto.ticket.TicketSummary;
import com.example.demo.model.Feedback;
import com.example.demo.service.CustomerAccountService;
import com.example.demo.service.DashboardService;
import com.example.demo.service.FeedbackService;
import com.example.demo.service.TicketService;
import com.example.demo.service.analytics.FeedbackThemeService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    @MockBean
    private CustomerAccountService customerAccountService;

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

        when(customerAccountService.list()).thenReturn(List.of(
                new CustomerAccountResponse(1L, "customer@test.com", "CUSTOMER")
        ));
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

    @Test
    void logFeedbackFormListsRegisteredCustomers() throws Exception {
        mockMvc.perform(get("/admin/feedback/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-feedback-new"))
                .andExpect(model().attributeExists("customers"));
    }

    @Test
    void logFeedbackAttributesEntryToCustomerAndRecordsAdmin() throws Exception {
        mockMvc.perform(post("/admin/feedback")
                        .with(csrf())
                        .param("customerEmail", "customer@test.com")
                        .param("category", "PRODUCT")
                        .param("project", "Reactor V2")
                        .param("account", "ACME")
                        .param("description", "Reported overheating by phone"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/feedback?logged"));

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackService).logFeedbackOnBehalf(
                captor.capture(), eq("customer@test.com"), eq("admin@test.com"));

        Feedback submitted = captor.getValue();
        assertEquals("PRODUCT", submitted.getCategory());
        assertEquals("Reported overheating by phone", submitted.getDescription());
    }

    @Test
    void logFeedbackRejectsEmailThatIsNotARegisteredCustomer() throws Exception {
        mockMvc.perform(post("/admin/feedback")
                        .with(csrf())
                        .param("customerEmail", "stranger@evil.com")
                        .param("category", "PRODUCT")
                        .param("project", "Reactor V2")
                        .param("account", "ACME")
                        .param("description", "Should not be stored"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/feedback/new?invalidCustomer"));

        verify(feedbackService, never())
                .logFeedbackOnBehalf(any(), anyString(), anyString());
    }

    @Test
    void logFeedbackMatchesCustomerEmailCaseInsensitively() throws Exception {
        mockMvc.perform(post("/admin/feedback")
                        .with(csrf())
                        .param("customerEmail", "Customer@Test.com")
                        .param("category", "SERVICE")
                        .param("project", "Field kit")
                        .param("account", "Beta")
                        .param("description", "Follow-up call"))
                .andExpect(redirectedUrl("/admin/feedback?logged"));

        verify(feedbackService).logFeedbackOnBehalf(
                any(), eq("Customer@Test.com"), eq("admin@test.com"));
    }
}
