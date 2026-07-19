package com.example.demo.dto.dashboard;

import java.util.List;

/**
 * A named, chart-ready dataset: an ordered list of {@link ChartPoint}s plus a
 * key and human label. Backend computes the buckets so the frontend only needs
 * to render.
 */
public record ChartSeries(String key, String label, List<ChartPoint> points) {
}
