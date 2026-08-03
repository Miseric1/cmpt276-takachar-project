package com.example.demo;

import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.model.DiagnosticOption;
import com.example.demo.model.DiagnosticQuestion;
import com.example.demo.model.Feedback;
import com.example.demo.model.PublicationStatus;
import com.example.demo.model.SentimentLabel;
import com.example.demo.repository.DiagnosticQuestionRepository;
import com.example.demo.repository.KnowledgeArticleRepository;
import com.example.demo.service.DashboardService;
import com.example.demo.service.FeedbackService;
import com.example.demo.service.KnowledgeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketingApiIntegrationTest {

    private static final String CUSTOMER = "customer@example.com";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired DiagnosticQuestionRepository questionRepository;
    @Autowired KnowledgeArticleRepository articleRepository;
    @Autowired KnowledgeService knowledgeService;
    @Autowired DashboardService dashboardService;
    @Autowired FeedbackService feedbackService;

    @Test
    void customerCreatesOwnedTicketAndAdminRunsLifecycle() throws Exception {
        long before = dashboardService.getTicketStatistics().total();
        JsonNode created = createTicket(CUSTOMER, "Unit stops after ten minutes", null);
        long ticketId = created.path("id").asLong();

        assertThat(created.path("referenceNumber").asText()).startsWith("TKT-");
        assertThat(created.path("status").asText()).isEqualTo("OPEN");
        assertThat(created.path("timeline").size()).isEqualTo(1);

        mockMvc.perform(get("/api/tickets/{id}", ticketId)
                        .with(user("other@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/tickets/{id}/assignment", ticketId)
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"department":"Technical Support","assigneeEmail":"agent@takachar.com","priority":"HIGH"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.department").value("Technical Support"))
                .andExpect(jsonPath("$.firstRespondedAt").isNotEmpty());

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\",\"note\":\"Remote reset completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.health").value("GREEN"));

        mockMvc.perform(patch("/api/tickets/{id}/status", ticketId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertThat(dashboardService.getTicketStatistics().total()).isEqualTo(before + 1);
        assertThat(dashboardService.getTicketStatistics().closed()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void diagnosticTreeSuggestsPublishedFaqAndEscalatesTrailIntoTicket() throws Exception {
        KnowledgeResponse article = knowledgeService.create(new KnowledgeRequest(
                "Diagnostic power reset", "Power reset steps", "Disconnect power for ten minutes and reconnect.",
                "Troubleshooting", Set.of("power"), Set.of(), "admin@test.com", PublicationStatus.PUBLISHED),
                "admin@test.com");

        DiagnosticQuestion child = new DiagnosticQuestion();
        child.setKey("test-power-detail");
        child.setPrompt("Does the power indicator turn on?");
        child.setCategory("Test diagnostics");
        child.setActive(true);
        child.setSuggestedArticle(articleRepository.findById(article.id()).orElseThrow());
        questionRepository.save(child);

        DiagnosticQuestion root = new DiagnosticQuestion();
        root.setKey("test-root");
        root.setPrompt("Which system is affected?");
        root.setCategory("Test diagnostics");
        root.setRootQuestion(true);
        root.setActive(true);
        DiagnosticOption option = new DiagnosticOption();
        option.setQuestion(root);
        option.setLabel("Power system");
        option.setValue("power");
        option.setDisplayOrder(1);
        option.setNextQuestion(child);
        root.getOptions().add(option);
        root = questionRepository.save(root);

        String startBody = mockMvc.perform(post("/api/diagnostics/sessions")
                        .param("category", "Test diagnostics")
                        .with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentQuestion.key").value("test-root"))
                .andReturn().getResponse().getContentAsString();
        JsonNode started = objectMapper.readTree(startBody);
        String sessionId = started.path("id").asText();
        long optionId = root.getOptions().get(0).getId();

        mockMvc.perform(post("/api/diagnostics/sessions/{id}/answers", sessionId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + root.getId() + ",\"optionId\":" + optionId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestion.key").value("test-power-detail"));

        mockMvc.perform(post("/api/diagnostics/sessions/{id}/answers", sessionId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + child.getId() + ",\"answerText\":\"No indicator light\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLUTION_SUGGESTED"))
                .andExpect(jsonPath("$.suggestedArticle.id").value(article.id()));

        mockMvc.perform(post("/api/diagnostics/sessions/{id}/resolution", sessionId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resolved\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_TICKET"));

        JsonNode ticket = createTicket(CUSTOMER, "Power reset did not work", sessionId);
        assertThat(ticket.path("automaticResolutionAttempted").asBoolean()).isTrue();
        assertThat(ticket.path("suggestedArticle").path("id").asLong()).isEqualTo(article.id());
        assertThat(ticket.path("diagnosticTrail").size()).isEqualTo(2);
    }

    @Test
    void ownerCanUploadDownloadAndDeleteImagesWhileDocumentsAreRejected() throws Exception {
        long ticketId = createTicket(CUSTOMER, "Visible smoke near the vent", null).path("id").asLong();
        MockMultipartFile image = new MockMultipartFile("file", "smoke.png", "image/png",
                new byte[]{1, 2, 3, 4});

        String uploadBody = mockMvc.perform(multipart("/api/tickets/{id}/attachments", ticketId)
                        .file(image).with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("smoke.png"))
                .andReturn().getResponse().getContentAsString();
        long attachmentId = objectMapper.readTree(uploadBody).path("id").asLong();

        mockMvc.perform(get("/api/tickets/{id}/attachments/{attachmentId}", ticketId, attachmentId)
                        .with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{1, 2, 3, 4}));

        MockMultipartFile document = new MockMultipartFile("file", "notes.pdf", "application/pdf",
                new byte[]{5, 6});
        mockMvc.perform(multipart("/api/tickets/{id}/attachments", ticketId)
                        .file(document).with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/tickets/{id}/attachments/{attachmentId}", ticketId, attachmentId)
                        .with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void feedbackPersistsNeutralFallbackWhenHuggingFaceTokenIsNotConfigured() {
        Feedback feedback = feedbackService.createFeedback(new Feedback(
                "Service", "Pilot", "Account", "The response was clear and helpful", CUSTOMER));
        assertThat(feedback.getSentiment()).isEqualTo(SentimentLabel.NEUTRAL);
        assertThat(feedback.getSentimentModel()).endsWith(":fallback");
        assertThat(feedback.getSentimentAnalyzedAt()).isNotNull();
    }

    @Test
    void ticketApisRequireLoginAndCustomersCannotUseAdminAssignment() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());

        long ticketId = createTicket(CUSTOMER, "Assignment security check", null).path("id").asLong();
        mockMvc.perform(patch("/api/tickets/{id}/assignment", ticketId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"department\":\"Technical Support\"}"))
                .andExpect(status().isForbidden());
    }

    private JsonNode createTicket(String customer, String subject, String diagnosticSessionId) throws Exception {
        String diagnostic = diagnosticSessionId == null ? "null" : "\"" + diagnosticSessionId + "\"";
        String response = mockMvc.perform(post("/api/tickets")
                        .with(user(customer).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":"%s","description":"Detailed issue description","project":"Pilot A",
                                 "priority":"MEDIUM","diagnosticSessionId":%s}
                                """.formatted(subject, diagnostic)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

}
