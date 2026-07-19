package com.example.demo.dto.dashboard;

/**
 * The system-wide summary cards at the top of the dashboard: the single numbers
 * staff want at a glance. Always reflects the latest database state.
 */
public record DashboardOverviewDto(
        // Tickets (from the ticket provider; zero until Ticketing ships)
        long totalTickets,
        long openTickets,
        long overdueTickets,
        // Feedback
        long totalFeedback,
        long openFeedback,
        // Content
        long totalFaqs,
        long publishedFaqs,
        long totalArticles,
        long publishedArticles,
        long knowledgeBaseViews,
        // People
        long totalCustomers,
        long totalStaff) {
}
