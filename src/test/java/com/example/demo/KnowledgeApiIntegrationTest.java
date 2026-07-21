package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack functional coverage for the Knowledge Base REST API: the real
 * security filter chain, controllers, services, and an in-memory H2 database
 * wired together, exercising the CRUD, publication, versioning, and engagement
 * flows end to end (as opposed to {@link ApiSecurityTest}, which only checks
 * authorization outcomes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class KnowledgeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String articleJson(String title, String status) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "summary", "A short summary.",
                "body", "How to resolve a common printer issue, step by step.",
                "category", "Hardware",
                "tags", java.util.List.of("printer", "setup"),
                "status", status));
    }

    
    @Test
    @WithMockUser(roles = "ADMIN")
    void fullArticleLifecycleThroughTheApi() throws Exception {
        // Create as draft.
        String createResponse = mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Printer troubleshooting", "DRAFT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(1))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        // Not visible on the public endpoint while still a draft.
        mockMvc.perform(get("/api/knowledge/" + id)).andExpect(status().isNotFound());

        // Publish it.
        mockMvc.perform(patch("/api/knowledge/" + id + "/status").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        // Now visible publicly, and the view is counted.
        mockMvc.perform(get("/api/knowledge/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewCount").value(1));

        // Edit creates a version snapshot and bumps the version number.
        String editJson = objectMapper.writeValueAsString(Map.of(
                "title", "Printer troubleshooting",
                "summary", "Updated summary.",
                "body", "An updated troubleshooting guide.",
                "category", "Hardware",
                "tags", java.util.List.of("printer")));
        mockMvc.perform(put("/api/knowledge/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get("/api/knowledge/" + id + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].versionNumber").value(1));

        // Engagement endpoints are public.
        mockMvc.perform(post("/api/knowledge/" + id + "/helpful")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/knowledge/admin/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helpfulCount").value(1));

        // Delete removes it.
        mockMvc.perform(delete("/api/knowledge/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/knowledge/admin/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingDuplicateTitleReturns409() throws Exception {
        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Unique article title", "DRAFT")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Unique article title", "DRAFT")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidStatusTransitionReturns409() throws Exception {
        String createResponse = mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Archived article", "ARCHIVED")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(patch("/api/knowledge/" + id + "/status").param("status", "HIDDEN"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void creatingWithBlankTitleReturns400WithFieldError() throws Exception {
        String badJson = objectMapper.writeValueAsString(Map.of(
                "title", "",
                "body", "Some body.",
                "category", "General"));

        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void publishedArticleAppearsInPublicSearchAndCategoryListing() throws Exception {
        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Searchable printer guide", "PUBLISHED")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/knowledge").param("keyword", "printer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Searchable printer guide"));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Hardware')]").exists());

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'printer')]").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void dashboardReflectsCreatedArticle() throws Exception {
        mockMvc.perform(post("/api/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Dashboard visible guide", "PUBLISHED")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedArticles").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
