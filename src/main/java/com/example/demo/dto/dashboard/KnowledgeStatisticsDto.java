package com.example.demo.dto.dashboard;

import java.util.List;
import java.util.Map;

/**
 * Knowledge Base analytics: counts by publication status, total views, and
 * most-viewed / recently-updated leaderboards.
 */
public record KnowledgeStatisticsDto(
        long total,
        long published,
        long draft,
        long archived,
        Map<String, Long> byStatus,
        long totalViews,
        List<PopularContent> mostViewed,
        List<PopularContent> recentlyUpdated) {
}
