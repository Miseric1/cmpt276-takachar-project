package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.ChartPoint;
import com.example.demo.dto.dashboard.ChartSeries;
import com.example.demo.dto.dashboard.TicketStatisticsDto;
import com.example.demo.model.DiagnosticSessionStatus;
import com.example.demo.model.SupportTicket;
import com.example.demo.model.TicketPriority;
import com.example.demo.model.TicketStatus;
import com.example.demo.repository.DiagnosticSessionRepository;
import com.example.demo.repository.SupportTicketRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketAnalyticsService implements TicketAnalyticsProvider {
    private final SupportTicketRepository ticketRepository;
    private final DiagnosticSessionRepository diagnosticSessionRepository;

    public TicketAnalyticsService(SupportTicketRepository ticketRepository,
                                  DiagnosticSessionRepository diagnosticSessionRepository) {
        this.ticketRepository = ticketRepository;
        this.diagnosticSessionRepository = diagnosticSessionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketStatisticsDto getStatistics() {
        List<SupportTicket> tickets = ticketRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime weekStart = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        Arrays.stream(TicketStatus.values()).forEach(status -> byStatus.put(status.name(), 0L));
        Map<String, Long> byPriority = new LinkedHashMap<>();
        Arrays.stream(TicketPriority.values()).forEach(priority -> byPriority.put(priority.name(), 0L));
        tickets.forEach(ticket -> {
            byStatus.computeIfPresent(ticket.getStatus().name(), (key, count) -> count + 1);
            byPriority.computeIfPresent(ticket.getPriority().name(), (key, count) -> count + 1);
        });

        long total = tickets.size();
        long resolved = byStatus.get(TicketStatus.RESOLVED.name());
        long closed = byStatus.get(TicketStatus.CLOSED.name());
        long open = tickets.stream().filter(ticket -> ticket.getStatus().isOpen()).count();
        long overdue = tickets.stream().filter(ticket -> ticket.getStatus().isOpen())
                .filter(ticket -> ticket.getTargetResolutionAt() != null
                        && ticket.getTargetResolutionAt().isBefore(now)).count();
        long resolvedToday = resolvedSince(tickets, today.atStartOfDay());
        long resolvedThisWeek = resolvedSince(tickets, weekStart);
        long resolvedThisMonth = resolvedSince(tickets, monthStart);

        Double averageFirstResponse = averageHours(tickets, true);
        Double averageResolution = averageHours(tickets, false);
        double resolutionRate = percentage(resolved + closed, total);
        long autoResolved = diagnosticSessionRepository.countByStatus(DiagnosticSessionStatus.RESOLVED_WITH_FAQ);
        long escalated = diagnosticSessionRepository.countByStatus(DiagnosticSessionStatus.ESCALATED);
        double automaticResolutionRate = percentage(autoResolved, autoResolved + escalated);

        ChartSeries statuses = new ChartSeries("tickets_by_status", "Tickets by status",
                byStatus.entrySet().stream().map(entry -> new ChartPoint(entry.getKey(), entry.getValue())).toList());
        ChartSeries priorities = new ChartSeries("tickets_by_priority", "Tickets by priority",
                byPriority.entrySet().stream().map(entry -> new ChartPoint(entry.getKey(), entry.getValue())).toList());

        return new TicketStatisticsDto(total, open, resolved, closed, overdue,
                byStatus, byPriority, resolvedToday, resolvedThisWeek, resolvedThisMonth,
                averageFirstResponse, averageResolution, resolutionRate, automaticResolutionRate,
                List.of(statuses, priorities));
    }

    private long resolvedSince(List<SupportTicket> tickets, LocalDateTime since) {
        return tickets.stream().filter(ticket -> ticket.getResolvedAt() != null)
                .filter(ticket -> !ticket.getResolvedAt().isBefore(since)).count();
    }

    private Double averageHours(List<SupportTicket> tickets, boolean firstResponse) {
        // Preserve each ticket's start/end pairing while keeping nulls out.
        double sum = 0;
        long count = 0;
        for (SupportTicket ticket : tickets) {
            LocalDateTime end = firstResponse ? ticket.getFirstRespondedAt() : ticket.getResolvedAt();
            if (ticket.getCreatedAt() != null && end != null) {
                sum += Duration.between(ticket.getCreatedAt(), end).toMinutes() / 60.0;
                count++;
            }
        }
        return count == 0 ? null : Math.round((sum / count) * 100.0) / 100.0;
    }

    private double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : Math.round((numerator * 10000.0 / denominator)) / 100.0;
    }
}
