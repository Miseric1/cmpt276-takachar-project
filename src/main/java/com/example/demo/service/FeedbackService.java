package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Feedback;
import com.example.demo.model.SubmissionType;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.service.sentiment.SentimentAnalysisService;
import com.example.demo.service.sentiment.SentimentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    public FeedbackService(
            FeedbackRepository feedbackRepository,
            SentimentAnalysisService sentimentAnalysisService
    ) {
        this.feedbackRepository = feedbackRepository;
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public List<Feedback> search(
            SubmissionType type,
            String sortBy,
            Sort.Direction direction
    ) {
        String property = switch (sortBy == null ? "createdAt" : sortBy) {
            case "type", "status", "category", "project",
                 "account", "createdAt", "updatedAt" ->
                    sortBy == null ? "createdAt" : sortBy;

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported feedback sort field: " + sortBy
                    );
        };

        Sort sort = Sort.by(
                direction == null ? Sort.Direction.DESC : direction,
                property
        );

        return type == null
                ? feedbackRepository.findAll(sort)
                : feedbackRepository.findByType(type, sort);
    }

    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    public Feedback getFeedbackForReview(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Feedback", id)
                );

        if ("OPEN".equalsIgnoreCase(feedback.getStatus())) {
            feedback.setStatus("REVIEWED");
            return feedbackRepository.save(feedback);
        }

        return feedback;
    }

    public Feedback createFeedback(Feedback feedback) {
        if (feedback.getStatus() == null || feedback.getStatus().isEmpty()) {
            feedback.setStatus("OPEN");
        }

        if (feedback.getType() == null) {
            feedback.setType(SubmissionType.FEEDBACK);
        }

        analyze(feedback);

        return feedbackRepository.save(feedback);
    }

    /**
     * Records feedback an admin collected from a customer offline (phone, email,
     * site visit). The entry is attributed to the customer so it appears in their
     * own submission list, while {@code loggedBy} preserves who actually entered it.
     */
    public Feedback logFeedbackOnBehalf(
            Feedback feedback,
            String customerEmail,
            String adminEmail
    ) {
        feedback.setCreatedBy(customerEmail);
        feedback.setLoggedBy(adminEmail);

        return createFeedback(feedback);
    }

    public Feedback updateFeedback(Long id, Feedback updatedFeedback) {
        return feedbackRepository.findById(id)
                .map(feedback -> {
                    feedback.setCategory(updatedFeedback.getCategory());

                    if (updatedFeedback.getType() != null) {
                        feedback.setType(updatedFeedback.getType());
                    }

                    feedback.setProject(updatedFeedback.getProject());
                    feedback.setAccount(updatedFeedback.getAccount());
                    feedback.setDescription(updatedFeedback.getDescription());

                    if (updatedFeedback.getStatus() != null) {
                        feedback.setStatus(updatedFeedback.getStatus());
                    }

                    analyze(feedback);

                    return feedbackRepository.save(feedback);
                })
                .orElseThrow(() ->
                        new ResourceNotFoundException("Feedback", id)
                );
    }

    public void deleteFeedback(Long id) {
        if (!feedbackRepository.existsById(id)) {
            throw new ResourceNotFoundException("Feedback", id);
        }

        feedbackRepository.deleteById(id);
    }

    public List<Feedback> getByUser(String username) {
        return feedbackRepository.findByCreatedBy(username);
    }

    public Feedback reanalyzeSentiment(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Feedback", id)
                );

        analyze(feedback);

        return feedbackRepository.save(feedback);
    }

    private void analyze(Feedback feedback) {
        if (sentimentAnalysisService == null) {
            return;
        }

        SentimentResult result =
                sentimentAnalysisService.analyze(feedback.getDescription());

        feedback.setSentiment(result.label());
        feedback.setSentimentConfidence(result.confidence());
        feedback.setSentimentModel(result.model());
        feedback.setSentimentAnalyzedAt(LocalDateTime.now());
    }
}