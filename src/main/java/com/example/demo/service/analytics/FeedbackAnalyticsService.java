package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.ChartSeries;
import com.example.demo.dto.dashboard.FeedbackStatisticsDto;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.util.TrendUtils;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analytics over customer feedback, computed from the existing Feedback data.
 * All aggregation is pushed into the database where practical; only the small
 * timestamp list for the trend chart is bucketed in memory.
 */
@Service
public class FeedbackAnalyticsService {

    private static final int TREND_DAYS = 14;

    private final FeedbackRepository feedbackRepository;

    public FeedbackAnalyticsService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public long total() {
        return feedbackRepository.count();
    }

    @Transactional(readOnly = true)
    public long open() {
        return feedbackRepository.countByStatus("OPEN");
    }

    @Transactional(readOnly = true)
    public FeedbackStatisticsDto getStatistics() {
        long total = feedbackRepository.count();
        Map<String, Long> byStatus = toCountMap(feedbackRepository.countGroupedByStatus());
        Map<String, Long> byCategory = toCountMap(feedbackRepository.countGroupedByCategory());

        LocalDateTime now = LocalDateTime.now();
        long last7 = feedbackRepository.findCreatedAtSince(now.minusDays(7)).size();
        List<LocalDateTime> last30Timestamps = feedbackRepository.findCreatedAtSince(now.minusDays(30));
        long last30 = last30Timestamps.size();

        List<LocalDateTime> trendTimestamps = feedbackRepository.findCreatedAtSince(now.minusDays(TREND_DAYS));
        ChartSeries submissions = new ChartSeries("feedback_daily", "Feedback submitted per day",
                TrendUtils.dailyCounts(trendTimestamps, TREND_DAYS));

        long open = byStatus.getOrDefault("OPEN", 0L);
        long resolved = byStatus.getOrDefault("RESOLVED", 0L);

        return new FeedbackStatisticsDto(total, open, resolved, byStatus, byCategory,
                last7, last30, List.of(submissions));
    }

    /** Convert a {status/category, count} projection into an ordered map. */
    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] == null ? "UNSPECIFIED" : row[0].toString();
            map.put(key, ((Number) row[1]).longValue());
        }
        return map;
    }
}
