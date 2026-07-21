package com.example.demo.util;

import com.example.demo.dto.dashboard.ChartPoint;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrendUtilsTest {

    @Test
    void emitsOneZeroPointPerDayWhenNoTimestamps() {
        List<ChartPoint> points = TrendUtils.dailyCounts(List.of(), 7);

        assertThat(points).hasSize(7);
        assertThat(points).allMatch(p -> p.value() == 0);
    }

    @Test
    void labelsAreIsoDatesOldestFirstEndingToday() {
        List<ChartPoint> points = TrendUtils.dailyCounts(List.of(), 5);

        LocalDate today = LocalDate.now();
        assertThat(points.get(4).label()).isEqualTo(today.toString());
        assertThat(points.get(0).label()).isEqualTo(today.minusDays(4).toString());
    }

    @Test
    void countsMultipleTimestampsOnTheSameDay() {
        LocalDate today = LocalDate.now();
        List<LocalDateTime> timestamps = List.of(
                today.atTime(LocalTime.of(1, 0)),
                today.atTime(LocalTime.of(13, 30)),
                today.atTime(LocalTime.of(23, 59)));

        List<ChartPoint> points = TrendUtils.dailyCounts(timestamps, 3);

        ChartPoint todayPoint = points.get(points.size() - 1);
        assertThat(todayPoint.label()).isEqualTo(today.toString());
        assertThat(todayPoint.value()).isEqualTo(3);
    }

    @Test
    void bucketsTimestampsIntoTheCorrectDay() {
        LocalDate today = LocalDate.now();
        LocalDateTime yesterday = today.minusDays(1).atTime(9, 0);

        List<ChartPoint> points = TrendUtils.dailyCounts(List.of(yesterday), 3);

        ChartPoint yesterdayPoint = points.stream()
                .filter(p -> p.label().equals(today.minusDays(1).toString()))
                .findFirst().orElseThrow();
        assertThat(yesterdayPoint.value()).isEqualTo(1);

        ChartPoint todayPoint = points.stream()
                .filter(p -> p.label().equals(today.toString()))
                .findFirst().orElseThrow();
        assertThat(todayPoint.value()).isEqualTo(0);
    }

    @Test
    void ignoresTimestampsOutsideTheWindow() {
        LocalDate today = LocalDate.now();
        LocalDateTime tooOld = today.minusDays(30).atTime(9, 0);

        List<ChartPoint> points = TrendUtils.dailyCounts(List.of(tooOld), 7);

        assertThat(points).allMatch(p -> p.value() == 0);
    }

    @Test
    void filtersOutNullTimestamps() {
        List<LocalDateTime> timestamps = new ArrayList<>(Arrays.asList(LocalDateTime.now(), null));

        List<ChartPoint> points = TrendUtils.dailyCounts(timestamps, 1);

        assertThat(points).hasSize(1);
        assertThat(points.get(0).value()).isEqualTo(1);
    }

    @Test
    void singleDayWindowReturnsOnlyToday() {
        List<ChartPoint> points = TrendUtils.dailyCounts(List.of(), 1);

        assertThat(points).hasSize(1);
        assertThat(points.get(0).label()).isEqualTo(LocalDate.now().toString());
    }
}
