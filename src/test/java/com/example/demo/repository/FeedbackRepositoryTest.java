package com.example.demo.repository;

import com.example.demo.model.Feedback;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void shouldSaveAndRetrieveFeedback() {
        Feedback feedback = new Feedback("Bug", "ProjectA", "Account1", "UI glitch", "user123");
        feedbackRepository.save(feedback);

        List<Feedback> allFeedback = feedbackRepository.findAll();
        assertThat(allFeedback).hasSize(1);
        assertThat(allFeedback.get(0).getDescription()).isEqualTo("UI glitch");
    }

    @Test
    void shouldFindByStatus() {
        Feedback f1 = new Feedback("Bug", "P1", "A1", "D1", "U1");
        f1.setStatus("RESOLVED");
        feedbackRepository.save(f1);

        Feedback f2 = new Feedback("Feature", "P2", "A2", "D2", "U2");
        feedbackRepository.save(f2); // default OPEN

        List<Feedback> resolvedList = feedbackRepository.findByStatus("RESOLVED");
        assertThat(resolvedList).hasSize(1);
        assertThat(resolvedList.get(0).getCategory()).isEqualTo("Bug");
    }
}
