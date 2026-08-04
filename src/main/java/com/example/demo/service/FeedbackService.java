package com.example.demo.service;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Feedback;
import com.example.demo.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Autowired
    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
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

        return feedbackRepository.save(feedback);
    }

    public Feedback updateFeedback(Long id, Feedback updatedFeedback) {
        return feedbackRepository.findById(id)
                .map(feedback -> {
                    feedback.setCategory(updatedFeedback.getCategory());
                    feedback.setProject(updatedFeedback.getProject());
                    feedback.setAccount(updatedFeedback.getAccount());
                    feedback.setDescription(updatedFeedback.getDescription());
                    feedback.setStatus(updatedFeedback.getStatus());

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
}