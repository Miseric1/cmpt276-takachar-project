package com.example.demo.dto.knowledge;

import com.example.demo.dto.CategoryDto;
import com.example.demo.model.PublicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight read model for article listings and search results. Carries the
 * summary but not the full body, keeping list payloads small.
 */
public record KnowledgeSummary(
        Long id,
        String title,
        String summary,
        CategoryDto category,
        List<String> tags,
        PublicationStatus status,
        int estimatedReadingTimeMinutes,
        long viewCount,
        long helpfulCount,
        LocalDateTime updatedAt) {
}
