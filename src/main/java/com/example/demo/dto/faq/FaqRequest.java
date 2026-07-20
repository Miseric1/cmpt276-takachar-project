package com.example.demo.dto.faq;

import com.example.demo.model.PublicationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Create/update payload for an FAQ. The category is supplied by name and
 * resolved (or created) by the service, so the frontend never needs a category
 * id to author content. Status is optional; when omitted the FAQ stays a draft.
 */
public record FaqRequest(
        @NotBlank(message = "Question is required")
        @Size(max = 500, message = "Question must be at most 500 characters")
        String question,

        @NotBlank(message = "Answer is required")
        String answer,

        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        Set<String> tags,

        Integer displayOrder,

        PublicationStatus status) {
}
