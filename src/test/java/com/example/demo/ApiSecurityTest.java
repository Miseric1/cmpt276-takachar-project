package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the authorization rules added in SecurityConfig for the new REST API:
 * public reads, admin-only writes and dashboard, and 401 vs 403 semantics. Runs
 * with the full security filter chain active.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String faqJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "question", "Security test question?",
                "answer", "An answer.",
                "category", "General"));
    }

    @Test
    void publicCanListPublishedFaqs() throws Exception {
        mockMvc.perform(get("/api/faqs")).andExpect(status().isOk());
    }

    @Test
    void publicCanListCategoriesAndTags() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/tags")).andExpect(status().isOk());
    }

    @Test
    void unauthenticatedFaqCreateReturns401() throws Exception {
        mockMvc.perform(post("/api/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(faqJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerFaqCreateReturns403() throws Exception {
        mockMvc.perform(post("/api/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(faqJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateFaq() throws Exception {
        mockMvc.perform(post("/api/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(faqJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void unauthenticatedDashboardReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerDashboardReturns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanViewDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/overview")).andExpect(status().isOk());
    }
}
