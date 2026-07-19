package com.example.demo.controller;

import com.example.demo.dto.CategoryDto;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.faq.FaqResponse;
import com.example.demo.dto.faq.FaqSummary;
import com.example.demo.model.PublicationStatus;
import com.example.demo.service.FaqService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link FaqController}. Security filters are disabled here
 * (authorization is covered by {@code ApiSecurityTest}); this verifies routing,
 * request binding, validation, and the JSON response contract.
 */
@WebMvcTest(FaqController.class)
@AutoConfigureMockMvc(addFilters = false)
class FaqControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FaqService faqService;

    @Autowired
    private ObjectMapper objectMapper;

    private FaqResponse sampleResponse() {
        return new FaqResponse(1L, "How do I reset my password?", "From the account page.",
                new CategoryDto(3L, "Accounts", null), List.of("login", "password"), 1,
                PublicationStatus.PUBLISHED, "admin@test.com", "admin@test.com",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 1, 5, 2, 0);
    }

    @Test
    void listsPublishedFaqs() throws Exception {
        FaqSummary summary = new FaqSummary(1L, "How do I reset my password?",
                new CategoryDto(3L, "Accounts", null), List.of("login"), PublicationStatus.PUBLISHED,
                1, 5, 2, LocalDateTime.now());
        PageResponse<FaqSummary> page = new PageResponse<>(List.of(summary), 0, 20, 1, 1, false, false);
        when(faqService.searchPublished(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/faqs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getsPublishedFaqDetail() throws Exception {
        when(faqService.getPublishedByIdAndCountView(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/faqs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("How do I reset my password?"))
                .andExpect(jsonPath("$.category.name").value("Accounts"));
    }

    @Test
    void createsFaq() throws Exception {
        when(faqService.create(any(), any())).thenReturn(sampleResponse());
        Map<String, Object> body = Map.of(
                "question", "How do I reset my password?",
                "answer", "From the account page.",
                "category", "Accounts");

        mockMvc.perform(post("/api/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void rejectsInvalidFaq() throws Exception {
        // Missing required question/answer/category -> 400 with field errors.
        Map<String, Object> body = Map.of("question", "");

        mockMvc.perform(post("/api/faqs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void registersHelpfulVote() throws Exception {
        mockMvc.perform(post("/api/faqs/1/helpful"))
                .andExpect(status().isNoContent());
        verify(faqService).markHelpful(eq(1L));
    }
}
