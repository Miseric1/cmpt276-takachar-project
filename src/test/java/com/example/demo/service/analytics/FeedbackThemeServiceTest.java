package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.ThemeSummary;
import com.example.demo.model.Feedback;
import com.example.demo.model.SentimentLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackThemeServiceTest {

    private FeedbackThemeService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackThemeService();
    }

    private Feedback feedback(String description) {
        return feedback(description, "PRODUCT", null);
    }

    private Feedback feedback(String description, String category, SentimentLabel sentiment) {
        Feedback item = new Feedback(category, "Project", "Account", description, "user@test.com");
        item.setSentiment(sentiment);
        return item;
    }

    @Test
    void returnsEmptyListForEmptyInput() {
        assertTrue(service.extractThemes(Collections.emptyList(), 5).isEmpty());
        assertTrue(service.extractThemes(null, 5).isEmpty());
    }

    @Test
    void handlesNullAndBlankDescriptions() {
        List<Feedback> items = List.of(feedback(null), feedback("   "));

        assertTrue(service.extractThemes(items, 5).isEmpty());
    }

    @Test
    void ignoresStopWordsShortTokensAndNumbers() {
        List<Feedback> items = List.of(
                feedback("The it is 123 ok and overheating"),
                feedback("It was 456 an overheating the and")
        );

        List<ThemeSummary> themes = service.extractThemes(items, 10);

        assertEquals(1, themes.size());
        assertEquals("overheating", themes.get(0).term());
    }

    @Test
    void countsDistinctFeedbackItemsNotRepetitions() {
        List<Feedback> items = List.of(
                feedback("overheating overheating overheating"),
                feedback("unit is overheating")
        );

        List<ThemeSummary> themes = service.extractThemes(items, 10);

        assertEquals(1, themes.size());
        assertEquals(2, themes.get(0).mentionCount());
    }

    @Test
    void excludesTermsMentionedOnlyOnce() {
        List<Feedback> items = List.of(
                feedback("reactor overheating badly"),
                feedback("reactor smells strange")
        );

        List<ThemeSummary> themes = service.extractThemes(items, 10);

        assertEquals(1, themes.size());
        assertEquals("reactor", themes.get(0).term());
    }

    @Test
    void ordersByMentionCountThenAlphabetically() {
        List<Feedback> items = List.of(
                feedback("reactor caught overheating"),
                feedback("overheating inside reactor"),
                feedback("reactor melting slowly"),
                feedback("burner melting quick")
        );

        List<ThemeSummary> themes = service.extractThemes(items, 10);

        assertEquals("reactor", themes.get(0).term());
        assertEquals(3, themes.get(0).mentionCount());
        // melting (2) and overheating (2) tie -> alphabetical
        assertEquals("melting", themes.get(1).term());
        assertEquals("overheating", themes.get(2).term());
    }

    @Test
    void respectsLimit() {
        List<Feedback> items = List.of(
                feedback("alpha beta gamma"),
                feedback("alpha beta gamma")
        );

        assertEquals(2, service.extractThemes(items, 2).size());
    }

    @Test
    void bigramAbsorbsItsUnigramsWhenCoFrequent() {
        List<Feedback> items = List.of(
                feedback("battery life poor"),
                feedback("battery life short"),
                feedback("battery life disappointing")
        );

        List<ThemeSummary> themes = service.extractThemes(items, 10);
        List<String> terms = themes.stream().map(ThemeSummary::term).toList();

        assertTrue(terms.contains("battery life"));
        assertFalse(terms.contains("battery"));
        assertFalse(terms.contains("life"));
    }

    @Test
    void keepsUnigramWhenItIsMuchMoreCommonThanBigram() {
        List<Feedback> items = List.of(
                feedback("battery life poor"),
                feedback("battery drains fast"),
                feedback("battery swollen"),
                feedback("battery gets hot"),
                feedback("battery died again")
        );

        List<ThemeSummary> themes = service.extractThemes(items, 10);
        List<String> terms = themes.stream().map(ThemeSummary::term).toList();

        assertTrue(terms.contains("battery"));
        assertEquals(5, themes.stream()
                .filter(theme -> theme.term().equals("battery"))
                .findFirst().orElseThrow().mentionCount());
    }

    @Test
    void buildsCategoryAndSentimentBreakdowns() {
        List<Feedback> items = List.of(
                feedback("unit overheating", "PRODUCT", SentimentLabel.NEGATIVE),
                feedback("stove overheating", "TECHNICAL", SentimentLabel.NEGATIVE),
                feedback("overheating again", null, null)
        );

        ThemeSummary theme = service.extractThemes(items, 10).get(0);

        assertEquals("overheating", theme.term());
        assertEquals(3, theme.mentionCount());
        assertEquals(1L, theme.byCategory().get("PRODUCT"));
        assertEquals(1L, theme.byCategory().get("TECHNICAL"));
        assertEquals(1L, theme.byCategory().get("UNSPECIFIED"));
        assertEquals(2L, theme.bySentiment().get("NEGATIVE"));
        assertEquals(1L, theme.bySentiment().get("UNANALYZED"));
    }

    @Test
    void matchingIsCaseInsensitive() {
        List<Feedback> items = List.of(
                feedback("OVERHEATING unit"),
                feedback("Overheating stove")
        );

        List<ThemeSummary> themes = service.extractThemes(items, 10);

        assertEquals(2, themes.stream()
                .filter(theme -> theme.term().equals("overheating"))
                .findFirst().orElseThrow().mentionCount());
    }
}
