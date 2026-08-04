package com.example.demo.dto.dashboard;

import java.util.List;
import java.util.Map;

/**
 * Ticket analytics returned by the staff dashboard.
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

    /** Useful neutral value for isolated controller tests and empty datasets. */
    public static TicketStatisticsDto empty() {
        return new TicketStatisticsDto(0, 0, 0, 0, 0,
                Map.of(), Map.of(), 0, 0, 0,
                null, null, 0.0, 0.0, List.of());
    }
}
