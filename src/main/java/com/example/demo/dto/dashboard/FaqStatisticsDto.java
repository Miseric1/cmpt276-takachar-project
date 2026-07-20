package com.example.demo.dto.dashboard;

import java.util.List;
import java.util.Map;

/**
 * FAQ effectiveness analytics: counts by publication status, total views, and
 * popularity leaderboards.
 */
public record FaqStatisticsDto(
        long total,
        long published,
        long draft,
        long archived,
        Map<String, Long> byStatus,
        long totalViews,
        List<PopularContent> mostViewed,
        List<PopularContent> leastViewed,
        List<PopularContent> mostHelpful) {
}
