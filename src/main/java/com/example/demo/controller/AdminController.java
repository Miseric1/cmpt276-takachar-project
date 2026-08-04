package com.example.demo.controller;

import com.example.demo.dto.PageResponse;
import com.example.demo.dto.dashboard.TicketStatisticsDto;
import com.example.demo.dto.ticket.TicketSummary;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Feedback;
import com.example.demo.service.DashboardService;
import com.example.demo.service.FeedbackService;
import com.example.demo.service.TicketService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class AdminController {

    private static final int TICKET_PREVIEW_LIMIT = 5;

    private final FeedbackService feedbackService;
    private final TicketService ticketService;
    private final DashboardService dashboardService;

    public AdminController(FeedbackService feedbackService,
                            TicketService ticketService,
                            DashboardService dashboardService) {
        this.feedbackService = feedbackService;
        this.ticketService = ticketService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin/home")
    public String adminHome(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        List<Feedback> feedbackList = feedbackService.getAllFeedback();

        long openFeedback = feedbackList.stream()
                .filter(feedback ->
                        "OPEN".equalsIgnoreCase(feedback.getStatus())
                )
                .count();

        model.addAttribute("email", userDetails.getUsername());
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("totalFeedback", feedbackList.size());
        model.addAttribute("openFeedback", openFeedback);

        addTicketOverviewAttributes(userDetails, model);

        return "admin";
    }

    /**
     * Populates the two ticket stat cards and the "Support tickets" preview
     * panel on the dashboard. Uses the same TicketStatisticsDto the
     * /api/dashboard/tickets endpoint returns, and a small unfiltered page of
     * tickets (sorted soonest-due-first, then narrowed to open ones) for the
     * preview list — so both stay in sync with the real ticket data without
     * duplicating the aggregation logic.
     */
    private void addTicketOverviewAttributes(UserDetails userDetails, Model model) {
        TicketStatisticsDto ticketStats = dashboardService.getTicketStatistics();
        model.addAttribute("overdueTickets", ticketStats.overdue());
        model.addAttribute("resolvedTickets", ticketStats.resolved());

        PageResponse<TicketSummary> page = ticketService.search(
                null, null, null, null, null, null, null, null,
                PageRequest.of(0, 50, Sort.by("targetResolutionAt").ascending()),
                userDetails.getUsername(), true);

        List<TicketSummary> activeTickets = page.getContent().stream()
                .filter(ticket -> ticket.status().isOpen())
                .limit(TICKET_PREVIEW_LIMIT)
                .toList();

        model.addAttribute("activeTickets", activeTickets);
    }

    @GetMapping("/admin/feedback")
    public String feedbackTracker(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        List<Feedback> feedbackList = feedbackService.getAllFeedback();

        long openFeedback = feedbackList.stream()
                .filter(feedback ->
                        "OPEN".equalsIgnoreCase(feedback.getStatus())
                )
                .count();

        model.addAttribute("email", userDetails.getUsername());
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("totalFeedback", feedbackList.size());
        model.addAttribute("openFeedback", openFeedback);

        return "admin-feedback";
    }

    @PostMapping("/admin/feedback/{id}/review")
    public String reviewFeedback(@PathVariable Long id) {
        feedbackService.getFeedbackForReview(id);

        return "redirect:/admin/feedback/" + id;
    }

    @GetMapping("/admin/feedback/{id}")
    public String feedbackDetails(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        Feedback feedback = feedbackService.getFeedbackById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Feedback", id)
                );

        model.addAttribute("email", userDetails.getUsername());
        model.addAttribute("feedback", feedback);

        return "admin-feedback-details";
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

    @GetMapping("/admin/customers")
    public String customerAccounts(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        model.addAttribute("email", userDetails.getUsername());

        return "admin-customers";
    }

    @GetMapping("/admin/tree")
    public String diagnosticTree(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model
    ) {
        model.addAttribute("email", userDetails.getUsername());

        return "admin-tree";
    }
}
