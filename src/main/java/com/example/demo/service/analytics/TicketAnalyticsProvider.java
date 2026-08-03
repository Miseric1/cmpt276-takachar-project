package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.TicketStatisticsDto;

/**
 * Seam between the dashboard and ticket-domain analytics. Keeping this
 * interface lets the dashboard depend on the reporting contract rather than
 * ticket persistence details.
 */
public interface TicketAnalyticsProvider {

    TicketStatisticsDto getStatistics();
}
