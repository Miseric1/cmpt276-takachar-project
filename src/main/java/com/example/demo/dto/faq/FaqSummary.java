package com.example.demo.dto.faq;

import com.example.demo.dto.CategoryDto;
import com.example.demo.model.PublicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lightweight read model for FAQ listings and search results. Omits the full
 * answer body to keep list payloads small.
 */
public record FaqSummary(
        Long id,
        String question,
        CategoryDto category,
        List<String> tags,
        PublicationStatus status,
        int displayOrder,
        long viewCount,
        long helpfulCount,
        LocalDateTime updatedAt) {
}
