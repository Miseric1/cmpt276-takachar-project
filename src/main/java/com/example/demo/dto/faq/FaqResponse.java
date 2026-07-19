package com.example.demo.dto.faq;

import com.example.demo.dto.CategoryDto;
import com.example.demo.model.PublicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full read model for a single FAQ, including its answer body and analytics
 * counters. Returned by detail and write endpoints.
 */
public record FaqResponse(
        Long id,
        String question,
        String answer,
        CategoryDto category,
        List<String> tags,
        int displayOrder,
        PublicationStatus status,
        String createdBy,
        String lastModifiedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt,
        int version,
        long viewCount,
        long helpfulCount,
        long notHelpfulCount) {
}
