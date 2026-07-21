package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        categoryService = new CategoryService(categoryRepository);
    }

    @Test
    void findAllDelegatesToRepository() {
        Category c = new Category("General");
        when(categoryRepository.findAll()).thenReturn(List.of(c));

        List<Category> result = categoryService.findAll();

        assertThat(result).containsExactly(c);
    }

    @Test
    void getByIdReturnsCategoryWhenFound() {
        Category c = new Category("General");
        c.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThat(categoryService.getById(1L)).isSameAs(c);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrCreateByNameReturnsExistingCategory() {
        Category existing = new Category("Troubleshooting");
        when(categoryRepository.findByNameIgnoreCase("Troubleshooting")).thenReturn(Optional.of(existing));

        Category result = categoryService.getOrCreateByName("Troubleshooting");

        assertThat(result).isSameAs(existing);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getOrCreateByNameTrimsAndCreatesWhenMissing() {
        when(categoryRepository.findByNameIgnoreCase("Billing")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.getOrCreateByName("  Billing  ");

        assertThat(result.getName()).isEqualTo("Billing");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createTrimsNameAndPersists() {
        when(categoryRepository.existsByNameIgnoreCase("Hardware")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.create(new CategoryRequest("  Hardware  ", "Hardware issues"));

        assertThat(result.getName()).isEqualTo("Hardware");
        assertThat(result.getDescription()).isEqualTo("Hardware issues");
    }

    @Test
    void createThrowsOnDuplicateName() {
        when(categoryRepository.existsByNameIgnoreCase("Billing")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Billing", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateAllowsKeepingItsOwnName() {
        Category existing = new Category("Billing");
        existing.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByNameIgnoreCase("Billing")).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.update(1L, new CategoryRequest("Billing", "Updated desc"));

        assertThat(result.getDescription()).isEqualTo("Updated desc");
    }

    @Test
    void updateThrowsWhenNameTakenByAnotherCategory() {
        Category current = new Category("Billing");
        current.setId(1L);
        Category other = new Category("Hardware");
        other.setId(2L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(current));
        when(categoryRepository.findByNameIgnoreCase("Hardware")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("Hardware", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteThrowsWhenCategoryMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesExistingCategory() {
        Category existing = new Category("Billing");
        existing.setId(1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        categoryService.delete(1L);

        verify(categoryRepository).delete(existing);
    }
}
