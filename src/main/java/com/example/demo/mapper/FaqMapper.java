package com.example.demo.mapper;

import com.example.demo.dto.CategoryDto;
import com.example.demo.dto.faq.FaqResponse;
import com.example.demo.dto.faq.FaqSummary;
import com.example.demo.model.Faq;
import com.example.demo.model.Tag;

import java.util.List;

/**
 * Translates {@link Faq} entities into API DTOs. Must be called inside the
 * service transaction because it reads the lazy category and tag associations.
 */
public final class FaqMapper {

    private FaqMapper() {
    }

    public static FaqResponse toResponse(Faq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                CategoryDto.from(faq.getCategory()),
                tagNames(faq),
                faq.getDisplayOrder(),
                faq.getStatus(),
                faq.getCreatedBy(),
                faq.getLastModifiedBy(),
                faq.getCreatedAt(),
                faq.getUpdatedAt(),
                faq.getPublishedAt(),
                faq.getVersion(),
                faq.getViewCount(),
                faq.getHelpfulCount(),
                faq.getNotHelpfulCount());
    }

    public static FaqSummary toSummary(Faq faq) {
        return new FaqSummary(
                faq.getId(),
                faq.getQuestion(),
                CategoryDto.from(faq.getCategory()),
                tagNames(faq),
                faq.getStatus(),
                faq.getDisplayOrder(),
                faq.getViewCount(),
                faq.getHelpfulCount(),
                faq.getUpdatedAt());
    }

    private static List<String> tagNames(Faq faq) {
        return faq.getTags().stream().map(Tag::getName).sorted().toList();
    }
}
