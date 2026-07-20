package com.example.demo.service;

import com.example.demo.model.Tag;
import com.example.demo.repository.TagRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the shared tag vocabulary. Tags are normalised to lower case so
 * "Printer" and "printer" resolve to a single row, and missing tags are created
 * on demand when content references them.
 */
@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    /**
     * Turn a set of raw tag names into managed {@link Tag} entities, creating
     * any that do not exist yet. Blank names are ignored. Order is preserved.
     */
    @Transactional
    public Set<Tag> resolveTags(Set<String> rawNames) {
        Set<Tag> tags = new LinkedHashSet<>();
        if (rawNames == null) {
            return tags;
        }
        for (String raw : rawNames) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim().toLowerCase();
            Tag tag = tagRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> tagRepository.save(new Tag(name)));
            tags.add(tag);
        }
        return tags;
    }
}
