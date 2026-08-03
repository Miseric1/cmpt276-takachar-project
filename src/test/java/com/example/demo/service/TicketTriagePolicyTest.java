package com.example.demo.service;

import com.example.demo.model.TicketPriority;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTriagePolicyTest {

    private final TicketTriagePolicy policy = new TicketTriagePolicy(1, 2, 3, 5);

    @Test
    void targetDeadlinesSkipWeekends() {
        LocalDateTime friday = LocalDateTime.of(2026, 8, 7, 15, 30);

        assertThat(policy.targetFrom(friday, TicketPriority.URGENT))
                .isEqualTo(LocalDateTime.of(2026, 8, 10, 15, 30));
        assertThat(policy.targetFrom(friday, TicketPriority.HIGH))
                .isEqualTo(LocalDateTime.of(2026, 8, 11, 15, 30));
        assertThat(policy.targetFrom(friday, TicketPriority.MEDIUM))
                .isEqualTo(LocalDateTime.of(2026, 8, 12, 15, 30));
        assertThat(policy.targetFrom(friday, TicketPriority.LOW))
                .isEqualTo(LocalDateTime.of(2026, 8, 14, 15, 30));
    }

    @Test
    void weekendCreationStartsCountingOnMonday() {
        LocalDateTime saturday = LocalDateTime.of(2026, 8, 8, 10, 0);

        assertThat(policy.targetFrom(saturday, TicketPriority.URGENT))
                .isEqualTo(LocalDateTime.of(2026, 8, 10, 10, 0));
    }
}
