package com.example.demo.mapper;

import com.example.demo.dto.CategoryDto;
import com.example.demo.dto.knowledge.ArticleReference;
import com.example.demo.dto.knowledge.KnowledgeResponse;
import com.example.demo.dto.knowledge.KnowledgeSummary;
import com.example.demo.model.KnowledgeArticle;
import com.example.demo.model.Tag;

import java.util.List;

/**
 * Translates {@link KnowledgeArticle} entities into API DTOs. Must run inside
 * the service transaction because it reads lazy associations (category, tags,
 * related articles, contributors).
 */
public final class KnowledgeMapper {

    private KnowledgeMapper() {
    }

    public static KnowledgeResponse toResponse(KnowledgeArticle a) {
        return new KnowledgeResponse(
                a.getId(),
                a.getTitle(),
                a.getSummary(),
                a.getBody(),
                CategoryDto.from(a.getCategory()),
                tagNames(a),
                a.getRelatedArticles().stream().map(ArticleReference::from).toList(),
                a.getAuthor(),
                a.getContributors().stream().sorted().toList(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                a.getPublishedAt(),
                a.getVersion(),
                a.getEstimatedReadingTimeMinutes(),
                a.getViewCount(),
                a.getHelpfulCount(),
                a.getNotHelpfulCount(),
                a.getLastModifiedBy());
    }

    public static KnowledgeSummary toSummary(KnowledgeArticle a) {
        return new KnowledgeSummary(
                a.getId(),
                a.getTitle(),
                a.getSummary(),
                CategoryDto.from(a.getCategory()),
                tagNames(a),
                a.getStatus(),
                a.getEstimatedReadingTimeMinutes(),
                a.getViewCount(),
                a.getHelpfulCount(),
                a.getUpdatedAt());
    }

    private static List<String> tagNames(KnowledgeArticle a) {
        return a.getTags().stream().map(Tag::getName).sorted().toList();
    }
}
