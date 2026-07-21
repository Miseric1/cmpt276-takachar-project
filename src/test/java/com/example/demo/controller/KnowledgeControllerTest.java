package com.example.demo.controller;

import com.example.demo.dto.CategoryDto;
import com.example.demo.dto.PageResponse;
import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.dto.knowledge.KnowledgeVersionDto;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.InvalidStateException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.PublicationStatus;
import com.example.demo.service.KnowledgeService;

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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KnowledgeController.class)
@AutoConfigureMockMvc(addFilters = false)
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeService knowledgeService;

    @Autowired
    private ObjectMapper objectMapper;

    private KnowledgeResponse sampleResponse() {
        return new KnowledgeResponse(1L, "Setup guide", "Summary", "Body",
                new CategoryDto(1L, "General", null), List.of("network"), List.of(), "author@test.com",
                List.of("author@test.com"), PublicationStatus.PUBLISHED,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
                1, 2, 5, 1, 0, "author@test.com");
    }

    private KnowledgeSummary sampleSummary() {
        return new KnowledgeSummary(1L, "Setup guide", "Summary",
                new CategoryDto(1L, "General", null), List.of("network"), PublicationStatus.PUBLISHED,
                2, 5, 1, LocalDateTime.now());
    }

    private String requestJson() throws Exception {
        return objectMapper.writeValueAsString(
                new KnowledgeRequest("Setup guide", "Summary", "Body", "General",
                        Set.of("network"), null, "author@test.com", PublicationStatus.DRAFT));
    }

    @Test
    void listPublishedReturnsPage() throws Exception {
        PageResponse<KnowledgeSummary> page = new PageResponse<>(List.of(sampleSummary()), 0, 20, 1, 1, false, false);
        when(knowledgeService.searchPublished(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/knowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Setup guide"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPublishedReturnsArticle() throws Exception {
        when(knowledgeService.getPublishedByIdAndCountView(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Setup guide"));
    }

    @Test
    void getPublishedReturns404WhenMissing() throws Exception {
        when(knowledgeService.getPublishedByIdAndCountView(99L))
                .thenThrow(new ResourceNotFoundException("Article", 99L));

        mockMvc.perform(get("/api/knowledge/99")).andExpect(status().isNotFound());
    }

    @Test
    void markHelpfulReturnsNoContent() throws Exception {
        doNothing().when(knowledgeService).markHelpful(1L);

        mockMvc.perform(post("/api/knowledge/1/helpful")).andExpect(status().isNoContent());
    }

    @Test
    void markNotHelpfulReturnsNoContent() throws Exception {
        doNothing().when(knowledgeService).markNotHelpful(1L);

        mockMvc.perform(post("/api/knowledge/1/not-helpful")).andExpect(status().isNoContent());
    }

    @Test
    void listAllDelegatesToAdminSearch() throws Exception {
        PageResponse<KnowledgeSummary> page = new PageResponse<>(List.of(sampleSummary()), 0, 20, 1, 1, false, false);
        when(knowledgeService.search(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/knowledge/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getForAdminReturnsFullArticle() throws Exception {
        when(knowledgeService.getById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/knowledge/admin/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Body"));
    }

    @Test
    void getVersionsReturnsList() throws Exception {
        when(knowledgeService.getVersions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/knowledge/1/versions")).andExpect(status().isOk());
    }

    @Test
    void createReturns201() throws Exception {
        when(knowledgeService.create(any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createReturns409OnDuplicateTitle() throws Exception {
        when(knowledgeService.create(any(), any()))
                .thenThrow(new DuplicateResourceException("An article with this title already exists."));

        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void createReturns400OnBlankTitle() throws Exception {
        String badJson = objectMapper.writeValueAsString(
                new KnowledgeRequest("", "Summary", "Body", "General", Set.of(), null, null, null));

        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedArticle() throws Exception {
        when(knowledgeService.update(anyLong(), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/knowledge/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void changeStatusReturns409OnInvalidTransition() throws Exception {
        when(knowledgeService.changeStatus(anyLong(), any(), any()))
                .thenThrow(new InvalidStateException("Cannot change article status from ARCHIVED to HIDDEN."));

        mockMvc.perform(patch("/api/knowledge/1/status").param("status", "HIDDEN"))
                .andExpect(status().isConflict());
    }

    @Test
    void changeStatusReturnsUpdatedArticle() throws Exception {
        when(knowledgeService.changeStatus(anyLong(), any(), any())).thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/knowledge/1/status").param("status", "PUBLISHED"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(knowledgeService).delete(1L);

        mockMvc.perform(delete("/api/knowledge/1")).andExpect(status().isNoContent());
    }
}
