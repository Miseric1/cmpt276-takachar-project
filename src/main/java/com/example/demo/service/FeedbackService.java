package com.example.demo.service;

import com.example.demo.model.Feedback;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.service.sentiment.SentimentAnalysisService;
import com.example.demo.service.sentiment.SentimentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository,
                           SentimentAnalysisService sentimentAnalysisService) {
        this.feedbackRepository = feedbackRepository;
        this.sentimentAnalysisService = sentimentAnalysisService;
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    public Feedback createFeedback(Feedback feedback) {
        if (feedback.getStatus() == null || feedback.getStatus().isEmpty()) {
            feedback.setStatus("OPEN");
        }
        analyze(feedback);
        return feedbackRepository.save(feedback);
    }

    public Feedback updateFeedback(Long id, Feedback updatedFeedback) {
        return feedbackRepository.findById(id).map(feedback -> {
            feedback.setCategory(updatedFeedback.getCategory());
            feedback.setProject(updatedFeedback.getProject());
            feedback.setAccount(updatedFeedback.getAccount());
            feedback.setDescription(updatedFeedback.getDescription());
            feedback.setStatus(updatedFeedback.getStatus());
            analyze(feedback);
            // createdBy usually shouldn't change, but depends on logic
            return feedbackRepository.save(feedback);
        }).orElseThrow(() -> new RuntimeException("Feedback not found with id " + id));
    }

    public void deleteFeedback(Long id) {
        feedbackRepository.deleteById(id);
    }

    public List<Feedback> getByUser(String username) {
        return feedbackRepository.findByCreatedBy(username);
    }

    public Feedback reanalyzeSentiment(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id " + id));
        analyze(feedback);
        return feedbackRepository.save(feedback);
    }

    private void analyze(Feedback feedback) {
        if (sentimentAnalysisService == null) {
            return; // Keeps older isolated unit tests source-compatible.
        }
        SentimentResult result = sentimentAnalysisService.analyze(feedback.getDescription());
        feedback.setSentiment(result.label());
        feedback.setSentimentConfidence(result.confidence());
        feedback.setSentimentModel(result.model());
        feedback.setSentimentAnalyzedAt(LocalDateTime.now());
    }
}
