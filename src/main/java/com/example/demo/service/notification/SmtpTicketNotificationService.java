package com.example.demo.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
public class SmtpTicketNotificationService implements TicketNotificationService {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpTicketNotificationService(JavaMailSender mailSender,
                                         @Value("${app.notifications.from:support@takachar.com}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(TicketNotificationEvent notification) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(notification.recipient());
        message.setSubject(notification.subject());
        message.setText(notification.body());
        mailSender.send(message);
    }
}
