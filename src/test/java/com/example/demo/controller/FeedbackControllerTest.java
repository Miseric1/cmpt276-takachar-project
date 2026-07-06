package com.example.demo.controller;

import com.example.demo.model.Feedback;
import com.example.demo.service.FeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetAllFeedback() throws Exception {
        Feedback f1 = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        f1.setId(1L);
        when(feedbackService.getAllFeedback()).thenReturn(Arrays.asList(f1));

        mockMvc.perform(get("/api/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].description").value("Desc1"));
    }

    @Test
    void shouldGetFeedbackById() throws Exception {
        Feedback f1 = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        f1.setId(1L);
        when(feedbackService.getFeedbackById(1L)).thenReturn(Optional.of(f1));

        mockMvc.perform(get("/api/feedback/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Desc1"));
    }

    @Test
    void shouldCreateFeedback() throws Exception {
        Feedback input = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        Feedback saved = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        saved.setId(1L);
        saved.setStatus("OPEN");

        when(feedbackService.createFeedback(any(Feedback.class))).thenReturn(saved);

        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldUpdateFeedback() throws Exception {
        Feedback updateData = new Feedback();
        updateData.setDescription("Updated Desc");

        Feedback updated = new Feedback("Cat1", "Proj1", "Acc1", "Updated Desc", "User1");
        updated.setId(1L);

        when(feedbackService.updateFeedback(eq(1L), any(Feedback.class))).thenReturn(updated);

        mockMvc.perform(put("/api/feedback/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Desc"));
    }

    @Test
    void shouldDeleteFeedback() throws Exception {
        mockMvc.perform(delete("/api/feedback/1"))
                .andExpect(status().isNoContent());
    }
}
