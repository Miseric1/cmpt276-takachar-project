package com.example.demo.dto.knowledge;

import com.example.demo.model.KnowledgeArticleVersion;

import java.time.LocalDateTime;

/**
 * Read model for a historical article revision.
 */
public record KnowledgeVersionDto(
        Long id,
        Long articleId,
        int versionNumber,
        String title,
        String summary,
        String body,
        String categoryName,
        String status,
        String editedBy,
        LocalDateTime editedAt) {

    public static KnowledgeVersionDto from(KnowledgeArticleVersion v) {
        return new KnowledgeVersionDto(v.getId(), v.getArticleId(), v.getVersionNumber(), v.getTitle(),
                v.getSummary(), v.getBody(), v.getCategoryName(), v.getStatus(), v.getEditedBy(), v.getEditedAt());
    }
}
