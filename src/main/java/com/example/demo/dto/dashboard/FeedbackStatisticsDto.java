package com.example.demo.dto.dashboard;

import java.util.List;
import java.util.Map;

/**
 * Customer-feedback analytics: totals, distribution by status and category, and
 * recent submission volume, plus a chart-ready submissions-over-time series and
 * the top recurring themes mined from feedback descriptions.
 */
public record FeedbackStatisticsDto(
        long total,
        long open,
        long resolved,
        Map<String, Long> byStatus,
        Map<String, Long> byCategory,
        long submittedLast7Days,
        long submittedLast30Days,
        List<ChartSeries> charts,
        List<ThemeSummary> topThemes) {
}
