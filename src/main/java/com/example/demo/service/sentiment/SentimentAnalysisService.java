package com.example.demo.service.sentiment;

import com.example.demo.model.SentimentLabel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * Calls Hugging Face's serverless text-classification API. Feedback submission
 * must never be lost because a third-party model is unavailable, so an absent
 * token or transient API failure produces an explicit neutral fallback.
 */
@Service
public class SentimentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisService.class);

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String token;
    private final String endpoint;
    private final String model;

    public SentimentAnalysisService(
            ObjectMapper objectMapper,
            @Value("${app.huggingface.token:}") String token,
            @Value("${app.huggingface.base-url:https://router.huggingface.co/hf-inference/models}") String baseUrl,
            @Value("${app.huggingface.sentiment-model:cardiffnlp/twitter-roberta-base-sentiment-latest}") String model,
            @Value("${app.huggingface.connect-timeout-seconds:5}") int connectTimeoutSeconds,
            @Value("${app.huggingface.read-timeout-seconds:15}") int readTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.token = token == null ? "" : token.trim();
        this.model = model;
        this.endpoint = stripTrailingSlash(baseUrl) + "/" + model;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)));
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, readTimeoutSeconds)));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public SentimentResult analyze(String text) {
        if (text == null || text.isBlank() || token.isBlank()) {
            return fallback();
        }

        try {
            String body = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(Map.of("inputs", text, "parameters", Map.of("top_k", 3)))
                    .retrieve()
                    .body(String.class);
            return parse(body);
        } catch (Exception ex) {
            log.warn("Hugging Face sentiment analysis failed; storing neutral fallback: {}", ex.getMessage());
            return fallback();
        }
    }

    private SentimentResult parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root;
        if (root.isArray() && root.size() == 1 && root.get(0).isArray()) {
            results = root.get(0);
        }
        if (!results.isArray() || results.isEmpty()) {
            throw new IllegalArgumentException("Unexpected sentiment response shape");
        }

        JsonNode best = null;
        for (JsonNode candidate : results) {
            if (best == null || candidate.path("score").asDouble() > best.path("score").asDouble()) {
                best = candidate;
            }
        }

        String rawLabel = best.path("label").asText();
        SentimentLabel label = mapLabel(rawLabel);
        return new SentimentResult(label, best.path("score").asDouble(), model);
    }

    private SentimentLabel mapLabel(String rawLabel) {
        String normalized = rawLabel == null ? "" : rawLabel.trim().toLowerCase();
        if (normalized.contains("positive") || normalized.equals("label_2")) {
            return SentimentLabel.POSITIVE;
        }
        if (normalized.contains("negative") || normalized.equals("label_0")) {
            return SentimentLabel.NEGATIVE;
        }
        return SentimentLabel.NEUTRAL;
    }

    private SentimentResult fallback() {
        return new SentimentResult(SentimentLabel.NEUTRAL, null, model + ":fallback");
    }

    private static String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
