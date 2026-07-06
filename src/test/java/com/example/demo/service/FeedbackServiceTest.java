package com.example.demo.service;

import com.example.demo.model.Feedback;
import com.example.demo.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnAllFeedback() {
        Feedback f1 = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        Feedback f2 = new Feedback("Cat2", "Proj2", "Acc2", "Desc2", "User2");
        when(feedbackRepository.findAll()).thenReturn(Arrays.asList(f1, f2));

        List<Feedback> result = feedbackService.getAllFeedback();

        assertThat(result).hasSize(2);
        verify(feedbackRepository, times(1)).findAll();
    }

    @Test
    void shouldCreateFeedbackWithDefaultStatus() {
        Feedback input = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        input.setStatus(null); // Explicitly null
        
        Feedback saved = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        saved.setId(1L);
        saved.setStatus("OPEN");
        
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(saved);

        Feedback result = feedbackService.createFeedback(input);

        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void shouldUpdateFeedback() {
        Feedback existing = new Feedback("Cat1", "Proj1", "Acc1", "Desc1", "User1");
        existing.setId(1L);
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(existing);

        Feedback updateData = new Feedback();
        updateData.setDescription("New Desc");
        updateData.setStatus("RESOLVED");

        Feedback result = feedbackService.updateFeedback(1L, updateData);

        assertThat(result.getDescription()).isEqualTo("New Desc");
        assertThat(result.getStatus()).isEqualTo("RESOLVED");
    }
}
