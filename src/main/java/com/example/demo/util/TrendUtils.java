package com.example.demo.util;

import com.example.demo.dto.dashboard.ChartPoint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Small helpers for turning raw timestamps into chart-ready daily buckets, so
 * every analytics service produces trend series the same way.
 */
public final class TrendUtils {

    private TrendUtils() {
    }

    /**
     * Bucket timestamps into per-day counts for the last {@code days} days,
     * inclusive of today. Days with no data are emitted as zero so the series is
     * continuous and ready to plot. Labels are ISO dates (yyyy-MM-dd).
     */
    public static List<ChartPoint> dailyCounts(List<LocalDateTime> timestamps, int days) {
        Map<LocalDate, Long> counts = timestamps.stream()
                .filter(java.util.Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        LocalDate today = LocalDate.now();
        List<ChartPoint> points = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            points.add(new ChartPoint(day.toString(), counts.getOrDefault(day, 0L)));
        }
        return points;
    }
}
