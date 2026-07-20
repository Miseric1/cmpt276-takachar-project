package com.example.demo.dto.dashboard;

import java.util.List;
import java.util.Map;

/**
 * Ticket analytics. The Ticketing module is not implemented in this iteration,
 * so a no-op provider currently returns an empty/zeroed instance. The shape is
 * final, however, so the future ticket module can populate it without any
 * change to the dashboard API contract or the frontend.
 */
public record TicketStatisticsDto(
        long total,
        long open,
        long resolved,
        long closed,
        long overdue,
        Map<String, Long> byStatus,
        Map<String, Long> byPriority,
        long resolvedToday,
        long resolvedThisWeek,
        long resolvedThisMonth,
        Double averageFirstResponseTimeHours,
        Double averageResolutionTimeHours,
        double resolutionRate,
        double automaticResolutionRate,
        List<ChartSeries> charts) {

    /** The zeroed placeholder used until the Ticketing module is implemented. */
    public static TicketStatisticsDto empty() {
        return new TicketStatisticsDto(0, 0, 0, 0, 0,
                Map.of(), Map.of(), 0, 0, 0,
                null, null, 0.0, 0.0, List.of());
    }
}
