package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.TicketStatisticsDto;

import org.springframework.stereotype.Service;

/**
 * Default, zeroed ticket analytics used until the Ticketing module is built.
 *
 * When a future ticket module adds its own {@link TicketAnalyticsProvider}, it
 * should annotate that bean with {@code @Primary} (or this placeholder should be
 * removed) so the dashboard begins reporting real ticket data with no other
 * change to the dashboard service, its DTOs, or the frontend.
 */
@Service
public class EmptyTicketAnalyticsProvider implements TicketAnalyticsProvider {

    @Override
    public TicketStatisticsDto getStatistics() {
        return TicketStatisticsDto.empty();
    }
}
