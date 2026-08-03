package com.example.demo.repository;

import com.example.demo.model.Feedback;
import com.example.demo.model.SubmissionType;
import org.springframework.data.domain.Sort;
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

    @Test
    void shouldStoreFeedbackAndComplaintsTogetherAndFilterByType() {
        feedbackRepository.save(new Feedback("Product", "P1", "A1", "Suggestion", "U1"));
        feedbackRepository.save(new Feedback("Service", "P2", "A2", "Missed pickup", "U2",
                SubmissionType.COMPLAINT));

        List<Feedback> complaints = feedbackRepository.findByType(
                SubmissionType.COMPLAINT, Sort.by("createdAt"));

        assertThat(complaints).hasSize(1);
        assertThat(complaints.get(0).getDescription()).isEqualTo("Missed pickup");
    }
}
