package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionNotificationConfigurationTest {

    @Test
    void productionDefaultsToLoggingWhenEmailProviderIsNotConfigured() throws IOException {
        StandardEnvironment environment = environmentWithProductionProperties();

        assertThat(environment.getProperty("app.notifications.provider")).isEqualTo("log");
    }

    @Test
    void productionStillAllowsResendToBeSelectedExplicitly() throws IOException {
        StandardEnvironment environment = environmentWithProductionProperties();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "deployment",
                Map.of("EMAIL_PROVIDER", "resend")));

        assertThat(environment.getProperty("app.notifications.provider")).isEqualTo("resend");
    }

    private StandardEnvironment environmentWithProductionProperties() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addLast(new ResourcePropertySource(
                "production",
                new ClassPathResource("application-prod.properties")));
        return environment;
    }
}
