package com.example.demo.dto.knowledge;

import com.example.demo.dto.CategoryDto;
import com.example.demo.model.PublicationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full read model for a single Knowledge Base article.
 */
public record KnowledgeResponse(
        Long id,
        String title,
        String summary,
        String body,
        CategoryDto category,
        List<String> tags,
        List<ArticleReference> relatedArticles,
        String author,
        List<String> contributors,
        PublicationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt,
        int version,
        int estimatedReadingTimeMinutes,
        long viewCount,
        long helpfulCount,
        long notHelpfulCount,
        String lastModifiedBy) {
}
