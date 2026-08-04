package com.example.demo.dto.dashboard;

import java.util.Map;

/**
 * A recurring theme extracted from feedback descriptions.
 *
 * @param term         the recurring word or two-word phrase, lowercase
 * @param mentionCount number of distinct feedback items mentioning the term
 * @param byCategory   feedback category -> count of mentioning items
 * @param bySentiment  sentiment label (or "UNANALYZED") -> count of
 *                     mentioning items
 */
public record ThemeSummary(
        String term,
        long mentionCount,
        Map<String, Long> byCategory,
        Map<String, Long> bySentiment
) {
}
