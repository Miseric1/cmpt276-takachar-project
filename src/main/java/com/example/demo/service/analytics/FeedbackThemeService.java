package com.example.demo.service.analytics;

import com.example.demo.dto.dashboard.ThemeSummary;
import com.example.demo.model.Feedback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts recurring themes from feedback descriptions with plain keyword
 * analysis: tokenize, drop stop-words, then count how many distinct feedback
 * items mention each term (document frequency, so a term repeated many times
 * in one long description still counts once). Adjacent-word bigrams are
 * counted too, and a bigram absorbs its constituent words when it appears
 * nearly as often ("battery life" suppresses "battery").
 */
@Service
public class FeedbackThemeService {

    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MIN_MENTIONS = 2;

    /**
     * Fraction of a unigram's mentions a containing bigram must reach before
     * the bigram replaces the unigram in the results.
     */
    private static final double BIGRAM_ABSORB_RATIO = 0.6;

    private static final Set<String> STOP_WORDS = Set.of(
            // articles, pronouns, conjunctions, prepositions
            "the", "and", "for", "are", "but", "not", "you", "all", "any",
            "can", "had", "her", "was", "one", "our", "out", "day", "get",
            "has", "him", "his", "how", "man", "new", "now", "old", "see",
            "two", "way", "who", "did", "its", "let", "put", "say", "she",
            "too", "use", "that", "with", "have", "this", "will", "your",
            "from", "they", "know", "want", "been", "much", "some", "time",
            "very", "when", "come", "here", "just", "like", "long", "make",
            "many", "more", "most", "over", "such", "take", "than", "them",
            "well", "were", "what", "into", "also", "back", "only", "then",
            "there", "their", "these", "thing", "think", "would", "could",
            "should", "about", "after", "again", "before", "being", "between",
            "both", "does", "doing", "down", "during", "each", "other",
            "which", "while", "where", "because", "having", "still", "since",
            "until", "upon", "under", "cannot", "even", "every", "ever",
            "though", "through", "however", "always", "never", "same",
            // auxiliaries / contractions after apostrophe stripping
            "don't", "doesn't", "didn't", "can't", "won't", "isn't", "aren't",
            "wasn't", "weren't", "it's", "i'm", "i've", "we're", "they're",
            // domain noise that would otherwise dominate every theme list
            "feedback", "please", "really", "thanks", "thank", "issue",
            "issues", "problem", "problems", "product", "customer", "team",
            "hello", "regards", "need", "needs", "using", "used", "getting",
            "keeps", "keep", "seems", "works", "working", "work"
    );

    /**
     * Returns the top recurring themes across the given feedback, most
     * mentioned first. A theme must be mentioned by at least
     * {@value MIN_MENTIONS} distinct feedback items to appear.
     */
    public List<ThemeSummary> extractThemes(List<Feedback> feedbackList, int limit) {
        if (feedbackList == null || feedbackList.isEmpty() || limit <= 0) {
            return List.of();
        }

        Map<String, List<Feedback>> mentions = new HashMap<>();

        for (Feedback feedback : feedbackList) {
            for (String term : termsOf(feedback)) {
                mentions.computeIfAbsent(term, key -> new ArrayList<>())
                        .add(feedback);
            }
        }

        mentions.values().removeIf(items -> items.size() < MIN_MENTIONS);
        mergeOverlappingTerms(mentions);

        return mentions.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, List<Feedback>> entry)
                                -> entry.getValue().size())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> toSummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * The distinct terms (unigrams and adjacent bigrams) mentioned by one
     * feedback item. Returning a set is what makes the final counts document
     * frequencies rather than raw term frequencies.
     */
    private Set<String> termsOf(Feedback feedback) {
        String description = feedback.getDescription();
        if (description == null || description.isBlank()) {
            return Set.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String raw : description.toLowerCase().split("[^a-z0-9']+")) {
            String token = raw.replaceAll("^'+|'+$", "");

            if (token.length() < MIN_TOKEN_LENGTH
                    || token.chars().allMatch(Character::isDigit)
                    || STOP_WORDS.contains(token)) {
                continue;
            }

            tokens.add(token);
        }

        Set<String> terms = new LinkedHashSet<>(tokens);
        for (int i = 0; i < tokens.size() - 1; i++) {
            terms.add(tokens.get(i) + " " + tokens.get(i + 1));
        }

        return terms;
    }

    /**
     * When a bigram is mentioned nearly as often as one of its constituent
     * unigrams, the bigram is the better label — drop the unigram so the
     * theme list doesn't show both "battery" and "battery life".
     */
    private void mergeOverlappingTerms(Map<String, List<Feedback>> mentions) {
        List<String> bigrams = mentions.keySet().stream()
                .filter(term -> term.indexOf(' ') >= 0)
                .toList();

        for (String bigram : bigrams) {
            long bigramCount = mentions.get(bigram).size();

            for (String word : bigram.split(" ")) {
                List<Feedback> wordMentions = mentions.get(word);
                if (wordMentions != null
                        && bigramCount >= wordMentions.size() * BIGRAM_ABSORB_RATIO) {
                    mentions.remove(word);
                }
            }
        }
    }

    private ThemeSummary toSummary(String term, List<Feedback> items) {
        return new ThemeSummary(
                term,
                items.size(),
                countBy(items, feedback -> feedback.getCategory() == null
                        ? "UNSPECIFIED" : feedback.getCategory()),
                countBy(items, feedback -> feedback.getSentiment() == null
                        ? "UNANALYZED" : feedback.getSentiment().name())
        );
    }

    private Map<String, Long> countBy(
            List<Feedback> items,
            java.util.function.Function<Feedback, String> classifier
    ) {
        Map<String, Long> counts = new HashMap<>();
        for (Feedback feedback : items) {
            counts.merge(classifier.apply(feedback), 1L, Long::sum);
        }

        Map<String, Long> ordered = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));

        return ordered;
    }
}
