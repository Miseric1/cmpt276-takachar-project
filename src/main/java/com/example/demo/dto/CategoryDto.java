package com.example.demo.dto;

import com.example.demo.model.Category;

/**
 * Read model for a {@link Category}. Never exposes the entity directly.
 */
public record CategoryDto(Long id, String name, String description) {

    public static CategoryDto from(Category category) {
        return new CategoryDto(category.getId(), category.getName(), category.getDescription());
    }
}
