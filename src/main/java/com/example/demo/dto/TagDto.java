package com.example.demo.dto;

import com.example.demo.model.Tag;

/**
 * Read model for a {@link Tag}.
 */
public record TagDto(Long id, String name) {

    public static TagDto from(Tag tag) {
        return new TagDto(tag.getId(), tag.getName());
    }
}
