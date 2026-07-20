package com.example.demo.dto.knowledge;

import com.example.demo.model.PublicationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Create/update payload for a Knowledge Base article. Category is supplied by
 * name (resolved or created by the service); related articles are supplied by
 * id. Reading time is derived server-side and never accepted from the client.
 */
public record KnowledgeRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 250, message = "Title must be at most 250 characters")
        String title,

        @Size(max = 1000, message = "Summary must be at most 1000 characters")
        String summary,

        @NotBlank(message = "Body is required")
        String body,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        Set<String> tags,

        Set<Long> relatedArticleIds,

        String author,

        PublicationStatus status) {
}
