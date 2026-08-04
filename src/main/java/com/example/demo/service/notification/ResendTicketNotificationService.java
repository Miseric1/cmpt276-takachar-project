package com.example.demo.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/** Sends provider-neutral ticket events through Resend's HTTPS email API. */
@Service
@ConditionalOnProperty(name = "app.notifications.provider", havingValue = "resend")
public class ResendTicketNotificationService implements TicketNotificationService {

    private final RestClient restClient;
    private final String from;

    public ResendTicketNotificationService(
            RestClient.Builder builder,
            @Value("${app.notifications.resend.api-key:}") String apiKey,
            @Value("${app.notifications.resend.base-url:https://api.resend.com}") String baseUrl,
            @Value("${app.notifications.from:Takachar Support <onboarding@resend.dev>}") String from) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "Resend is selected but app.notifications.resend.api-key is empty.");
        }
        this.from = from;
        this.restClient = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                .defaultHeader(HttpHeaders.USER_AGENT, "takachar-support/1.0")
                .build();
    }

    @Override
    public void send(TicketNotificationEvent notification) {
        ResendEmailRequest request = new ResendEmailRequest(
                from,
                List.of(notification.recipient()),
                notification.subject(),
                notification.body());

        restClient.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String text) {
    }
}
