package com.example.demo.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.notifications.provider", havingValue = "log", matchIfMissing = true)
public class LoggingTicketNotificationService implements TicketNotificationService {
    private static final Logger log = LoggerFactory.getLogger(LoggingTicketNotificationService.class);

    @Override
    public void send(TicketNotificationEvent notification) {
        log.info("Ticket email disabled; notification queued for {} with subject '{}'",
                notification.recipient(), notification.subject());
    }
}
