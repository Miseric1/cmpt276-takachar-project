package com.example.demo.controller;

import com.example.demo.model.Feedback;
import com.example.demo.service.FeedbackService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    private final FeedbackService feedbackService;

    public AdminController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/admin/home")
    public String adminHome(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        List<Feedback> feedbackList = feedbackService.getAllFeedback();

        long openFeedback = feedbackList.stream()
                .filter(feedback -> "OPEN".equalsIgnoreCase(feedback.getStatus()))
                .count();

        model.addAttribute("email", userDetails.getUsername());
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("totalFeedback", feedbackList.size());
        model.addAttribute("openFeedback", openFeedback);

        return "admin";
    }

    @GetMapping("/admin/feedback")
    public String feedbackTracker(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        List<Feedback> feedbackList = feedbackService.getAllFeedback();

        long openFeedback = feedbackList.stream()
                .filter(feedback -> "OPEN".equalsIgnoreCase(feedback.getStatus()))
                .count();

        model.addAttribute("email", userDetails.getUsername());
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("totalFeedback", feedbackList.size());
        model.addAttribute("openFeedback", openFeedback);

        return "admin-feedback";
    }

    @GetMapping("/admin/faq")
    public String faqKnowledgeBase(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        model.addAttribute("email", userDetails.getUsername());

        return "admin-faq";
    }

    @GetMapping("/admin/tickets")
    public String supportTickets(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        model.addAttribute("email", userDetails.getUsername());

        return "admin-tickets";
    }
}