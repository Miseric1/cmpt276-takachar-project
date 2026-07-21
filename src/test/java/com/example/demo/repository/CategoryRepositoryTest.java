package com.example.demo.repository;

import com.example.demo.model.Category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findByNameIgnoreCaseMatchesRegardlessOfCase() {
        categoryRepository.save(new Category("Troubleshooting"));

        Optional<Category> found = categoryRepository.findByNameIgnoreCase("troubleshooting");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Troubleshooting");
    }

    @Test
    void findByNameIgnoreCaseReturnsEmptyWhenAbsent() {
        assertThat(categoryRepository.findByNameIgnoreCase("Missing")).isEmpty();
    }

    @Test
    void existsByNameIgnoreCaseIsTrueForMatchingName() {
        categoryRepository.save(new Category("Billing"));

        assertThat(categoryRepository.existsByNameIgnoreCase("BILLING")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Hardware")).isFalse();
    }

    @Test
    void createdAtIsStampedOnSave() {
        Category saved = categoryRepository.save(new Category("General"));

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
