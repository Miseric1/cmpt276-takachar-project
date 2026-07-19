package com.example.demo.dto.dashboard;

/**
 * A single labelled value in a chart series (for example one day's ticket
 * count, or one category's share). Chart-ready: the frontend plots it directly.
 */
public record ChartPoint(String label, long value) {
}
