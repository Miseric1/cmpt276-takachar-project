package com.example.demo.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TicketNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(TicketNotificationListener.class);
    private final TicketNotificationService notificationService;

    public TicketNotificationListener(TicketNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(TicketNotificationEvent event) {
        if (event.recipient() == null || event.recipient().isBlank()) return;
        try {
            notificationService.send(event);
        } catch (Exception ex) {
            // Ticket state is already committed; a mail outage cannot roll it back.
            log.error("Could not send ticket notification to {}: {}", event.recipient(), ex.getMessage());
        }
    }
}
