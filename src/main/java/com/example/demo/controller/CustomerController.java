package com.example.demo.controller;

import com.example.demo.model.Feedback;
import com.example.demo.service.FeedbackService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomerController {

    private final FeedbackService feedbackService;

    public CustomerController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/customer/home")
    public String customerHome(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        model.addAttribute("email", userDetails.getUsername());
        return "customer";
    }

    @PostMapping("/customer/feedback")
    public String submitFeedback(
            @RequestParam String category,
            @RequestParam String project,
            @RequestParam String account,
            @RequestParam String description,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Feedback feedback = new Feedback(
                category,
                project,
                account,
                description,
                userDetails.getUsername()
        );

        feedbackService.createFeedback(feedback);

        return "redirect:/customer/home?submitted";
    }
}