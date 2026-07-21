package com.example.demo.controller;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Category;
import com.example.demo.service.CategoryService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category category() {
        Category category = new Category("General");
        category.setId(1L);
        category.setDescription("General questions");
        return category;
    }

    @Test
    void listReturnsAllCategories() throws Exception {
        when(categoryService.findAll()).thenReturn(List.of(category()));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("General"));
    }

    @Test
    void getReturnsCategoryById() throws Exception {
        when(categoryService.getById(1L)).thenReturn(category());

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(categoryService.getById(99L)).thenThrow(new ResourceNotFoundException("Category", 99L));

        mockMvc.perform(get("/api/categories/99")).andExpect(status().isNotFound());
    }

    @Test
    void createReturns201() throws Exception {
        when(categoryService.create(any())).thenReturn(category());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("General", "General questions"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("General"));
    }

    @Test
    void createReturns409OnDuplicateName() throws Exception {
        when(categoryService.create(any()))
                .thenThrow(new DuplicateResourceException("A category named 'General' already exists."));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("General", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void createReturns400OnBlankName() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedCategory() throws Exception {
        when(categoryService.update(anyLong(), any())).thenReturn(category());

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("General", "Updated"))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1")).andExpect(status().isNoContent());
    }
}
