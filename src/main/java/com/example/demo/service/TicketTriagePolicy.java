package com.example.demo.service;

import com.example.demo.model.TicketPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Central policy for priority-based ticket deadlines.
 *
 * Keeping the SLA mapping in one component ensures ticket creation and later
 * reprioritisation use the same rules. Values remain configurable per
 * environment without changing application code.
 */
@Component
public class TicketTriagePolicy {

    private static final List<String> URGENT_SIGNALS = List.of(
            "fire", "smoke", "explosion", "injury", "dangerous", "emergency",
            "safety risk", "overheating", "overheated", "electric shock", "gas leak");
    private static final List<String> HIGH_SIGNALS = List.of(
            "outage", "offline", "production stopped", "production down", "cannot start",
            "can't start", "will not start", "won't start", "data loss", "system failure",
            "completely broken", "unusable");

    private final int urgentBusinessDays;
    private final int highBusinessDays;
    private final int mediumBusinessDays;
    private final int lowBusinessDays;

    public TicketTriagePolicy(
            @Value("${app.ticketing.sla-business-days.urgent:1}") int urgentBusinessDays,
            @Value("${app.ticketing.sla-business-days.high:2}") int highBusinessDays,
            @Value("${app.ticketing.sla-business-days.medium:3}") int mediumBusinessDays,
            @Value("${app.ticketing.sla-business-days.low:5}") int lowBusinessDays) {
        this.urgentBusinessDays = positive(urgentBusinessDays);
        this.highBusinessDays = positive(highBusinessDays);
        this.mediumBusinessDays = positive(mediumBusinessDays);
        this.lowBusinessDays = positive(lowBusinessDays);
    }

    public int businessDaysFor(TicketPriority priority) {
        return switch (priority == null ? TicketPriority.MEDIUM : priority) {
            case URGENT -> urgentBusinessDays;
            case HIGH -> highBusinessDays;
            case MEDIUM -> mediumBusinessDays;
            case LOW -> lowBusinessDays;
        };
    }

    /**
     * Promotes explicitly supplied priority when the issue text contains a
     * known operational or safety signal. Text triage never lowers a priority.
     */
    public TicketPriority triage(TicketPriority requested, String subject, String description) {
        TicketPriority selected = requested == null ? TicketPriority.MEDIUM : requested;
        String issue = ((subject == null ? "" : subject) + " "
                + (description == null ? "" : description)).toLowerCase(Locale.ROOT);
        TicketPriority inferred = containsAny(issue, URGENT_SIGNALS) ? TicketPriority.URGENT
                : containsAny(issue, HIGH_SIGNALS) ? TicketPriority.HIGH : selected;
        return rank(inferred) > rank(selected) ? inferred : selected;
    }

    public LocalDateTime targetFrom(LocalDateTime start, TicketPriority priority) {
        LocalDateTime target = start;
        int remaining = businessDaysFor(priority);
        while (remaining > 0) {
            target = target.plusDays(1);
            if (isBusinessDay(target.getDayOfWeek())) remaining--;
        }
        return target;
    }

    public boolean isExpedited(TicketPriority priority) {
        return priority == TicketPriority.URGENT || priority == TicketPriority.HIGH;
    }

    private int positive(int businessDays) {
        return Math.max(1, businessDays);
    }

    private boolean isBusinessDay(DayOfWeek day) {
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private boolean containsAny(String value, List<String> signals) {
        return signals.stream().anyMatch(signal -> containsSignal(value, signal));
    }

    private boolean containsSignal(String value, String signal) {
        int from = 0;
        while (from < value.length()) {
            int index = value.indexOf(signal, from);
            if (index < 0) return false;
            int end = index + signal.length();
            boolean startsAtBoundary = index == 0 || !Character.isLetterOrDigit(value.charAt(index - 1));
            boolean endsAtBoundary = end == value.length() || !Character.isLetterOrDigit(value.charAt(end));
            if (startsAtBoundary && endsAtBoundary) return true;
            from = index + 1;
        }
        return false;
    }

    private int rank(TicketPriority priority) {
        return switch (priority) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case URGENT -> 4;
        };
    }
}
