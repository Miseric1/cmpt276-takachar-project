package com.example.demo.dto.faq;

import com.example.demo.model.FaqVersion;

import java.time.LocalDateTime;

/**
 * Read model for a historical FAQ revision.
 */
public record FaqVersionDto(
        Long id,
        Long faqId,
        int versionNumber,
        String question,
        String answer,
        String categoryName,
        String status,
        String editedBy,
        LocalDateTime editedAt) {

    public static FaqVersionDto from(FaqVersion v) {
        return new FaqVersionDto(v.getId(), v.getFaqId(), v.getVersionNumber(), v.getQuestion(),
                v.getAnswer(), v.getCategoryName(), v.getStatus(), v.getEditedBy(), v.getEditedAt());
    }
}
