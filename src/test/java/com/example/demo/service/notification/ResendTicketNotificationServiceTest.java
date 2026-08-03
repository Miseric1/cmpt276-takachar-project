package com.example.demo.service.notification;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendTicketNotificationServiceTest {

    @Test
    void sendsTicketNotificationThroughResendApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ResendTicketNotificationService service = new ResendTicketNotificationService(
                builder, "re_test_key", "https://api.resend.test", "Takachar <support@example.com>");

        server.expect(requestTo("https://api.resend.test/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test_key"))
                .andExpect(header("User-Agent", "takachar-support/1.0"))
                .andExpect(header("Idempotency-Key", org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankString())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"from":"Takachar <support@example.com>","to":["spoc@example.com"],
                         "subject":"New support ticket","text":"Ticket details"}
                        """))
                .andRespond(withSuccess("{\"id\":\"email-123\"}", MediaType.APPLICATION_JSON));

        service.send(new TicketNotificationEvent(
                "spoc@example.com", "New support ticket", "Ticket details"));

        server.verify();
    }

    @Test
    void refusesToStartResendProviderWithoutApiKey() {
        assertThatThrownBy(() -> new ResendTicketNotificationService(
                RestClient.builder(), " ", "https://api.resend.com", "support@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-key is empty");
    }
}
