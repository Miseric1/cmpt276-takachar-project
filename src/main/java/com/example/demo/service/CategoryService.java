package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the shared category vocabulary used by FAQs and Knowledge articles.
 * Category names are unique (case-insensitive) and trimmed on the way in.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    /**
     * Resolve a category by name, creating it if it does not yet exist. Used by
     * the FAQ and Knowledge services so content can reference categories freely.
     */
    @Transactional
    public Category getOrCreateByName(String rawName) {
        String name = normalize(rawName);
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(new Category(name)));
    }

    @Transactional
    public Category create(CategoryRequest request) {
        String name = normalize(request.name());
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists.");
        }
        Category category = new Category(name);
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, CategoryRequest request) {
        Category category = getById(id);
        String name = normalize(request.name());
        categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("A category named '" + name + "' already exists.");
                });
        category.setName(name);
        category.setDescription(request.description());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);
        categoryRepository.delete(category);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
