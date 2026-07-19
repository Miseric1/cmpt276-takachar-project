package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.TicketStatisticsDto;

/**
 * Seam for ticket analytics. The Ticketing module is not part of this
 * iteration, so {@link EmptyTicketAnalyticsProvider} supplies a zeroed
 * implementation. When Ticketing ships, its own provider bean replaces the
 * empty one and the dashboard immediately reflects real ticket data with no
 * change to the dashboard service, its DTOs, or the frontend.
 */
public interface TicketAnalyticsProvider {

    TicketStatisticsDto getStatistics();
}
