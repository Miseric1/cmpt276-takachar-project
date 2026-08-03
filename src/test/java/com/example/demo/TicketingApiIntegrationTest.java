package com.example.demo;

import com.example.demo.dto.knowledge.KnowledgeRequest;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.model.DiagnosticNode;
import com.example.demo.model.DiagnosticOption;
import com.example.demo.model.Feedback;
import com.example.demo.model.PublicationStatus;
import com.example.demo.model.SentimentLabel;
import com.example.demo.repository.DiagnosticNodeRepository;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Set;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @Autowired DiagnosticNodeRepository nodeRepository;
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

        UUID resolutionId = UUID.randomUUID();
        DiagnosticNode resolution = DiagnosticNode.builder()
                .id(resolutionId).type("resolution").text("Disconnect power for ten minutes and reconnect.")
                .root(false).knowledgeArticleId(article.id()).options(List.of()).build();

        UUID childId = UUID.randomUUID();
        UUID childOptionId = UUID.randomUUID();
        DiagnosticOption childOption = DiagnosticOption.builder().id(childOptionId)
                .label("No indicator light").destinationNodeId(resolutionId).sortOrder(0).build();
        DiagnosticNode child = DiagnosticNode.builder()
                .id(childId).type("question").text("Does the power indicator turn on?")
                .root(false).options(List.of(childOption)).build();

        UUID rootId = UUID.randomUUID();
        UUID rootOptionId = UUID.randomUUID();
        DiagnosticOption rootOption = DiagnosticOption.builder().id(rootOptionId)
                .label("Power system").destinationNodeId(childId).sortOrder(0).build();
        DiagnosticNode root = DiagnosticNode.builder()
                .id(rootId).type("question").text("Which system is affected?")
                .root(true).options(List.of(rootOption)).build();
        nodeRepository.saveAll(List.of(root, child, resolution));

        String startBody = mockMvc.perform(post("/api/diagnostics/sessions")
                        .with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentQuestion.id").value(rootId.toString()))
                .andReturn().getResponse().getContentAsString();
        JsonNode started = objectMapper.readTree(startBody);
        String sessionId = started.path("id").asText();
        mockMvc.perform(post("/api/diagnostics/sessions/{id}/answers", sessionId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"" + rootId + "\",\"optionId\":\"" + rootOptionId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestion.id").value(childId.toString()));

        mockMvc.perform(post("/api/diagnostics/sessions/{id}/answers", sessionId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"" + childId + "\",\"optionId\":\"" + childOptionId +
                                "\",\"answerText\":\"No indicator light\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLUTION_SUGGESTED"))
                .andExpect(jsonPath("$.suggestedArticle.id").value(article.id()))
                .andExpect(jsonPath("$.suggestedResolution").value("Disconnect power for ten minutes and reconnect."));

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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void primaryTreeCanBeReplacedWhileDiagnosticSessionIsActive() throws Exception {
        UUID rootId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID resolutionId = UUID.randomUUID();
        String initialTree = treePayload(rootId, optionId, resolutionId, "Which component is affected?");

        mockMvc.perform(put("/api/tree")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialTree))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootId").value(rootId.toString()));

        String sessionBody = mockMvc.perform(post("/api/diagnostics/sessions")
                        .with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentQuestion.id").value(rootId.toString()))
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionBody).path("id").asText();

        mockMvc.perform(put("/api/tree")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(treePayload(rootId, optionId, resolutionId,
                                "Which component is affected right now?")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/diagnostics/sessions/{id}", sessionId)
                        .with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestion.id").value(rootId.toString()))
                .andExpect(jsonPath("$.currentQuestion.text").value("Which component is affected right now?"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void adminCanLinkPublishedFaqToResolutionAndSessionReceivesIt() throws Exception {
        KnowledgeResponse article = knowledgeService.create(new KnowledgeRequest(
                "Linked diagnostic FAQ", "Linked FAQ summary", "Follow the linked diagnostic instructions.",
                "Troubleshooting", Set.of("diagnostic"), Set.of(), "admin@test.com", PublicationStatus.PUBLISHED),
                "admin@test.com");
        UUID rootId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID resolutionId = UUID.randomUUID();
        String payload = treePayload(rootId, optionId, resolutionId,
                "Which component needs help?", article.id());

        mockMvc.perform(put("/api/tree")
                        .with(user("admin@test.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes['" + resolutionId + "'].knowledgeArticleId")
                        .value(article.id()));

        String sessionBody = mockMvc.perform(post("/api/diagnostics/sessions")
                        .with(user(CUSTOMER).roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionBody).path("id").asText();

        mockMvc.perform(post("/api/diagnostics/sessions/{id}/answers", sessionId)
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":\"" + rootId + "\",\"optionId\":\"" + optionId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLUTION_SUGGESTED"))
                .andExpect(jsonPath("$.suggestedArticle.id").value(article.id()));
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

        mockMvc.perform(put("/api/tree")
                        .with(user(CUSTOMER).roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rootId\":null,\"nodes\":{}}"))
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

    private String treePayload(UUID rootId, UUID optionId, UUID resolutionId, String question) {
        return treePayload(rootId, optionId, resolutionId, question, null);
    }

    private String treePayload(UUID rootId, UUID optionId, UUID resolutionId,
                               String question, Long knowledgeArticleId) {
        String articleJson = knowledgeArticleId == null ? "null" : knowledgeArticleId.toString();
        return """
                {
                  "rootId":"%s",
                  "nodes":{
                    "%s":{"type":"question","text":"%s","options":[
                      {"id":"%s","label":"Power system","nextId":"%s"}
                    ]},
                    "%s":{"type":"resolution","text":"Restart the unit.",
                           "knowledgeArticleId":%s,"options":[]}
                  }
                }
                """.formatted(rootId, rootId, question, optionId, resolutionId, resolutionId, articleJson);
    }

}
