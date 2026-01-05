package com.servantin.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servantin.api.dto.auth.LoginRequest;
import com.servantin.api.dto.auth.RegisterRequest;
import com.servantin.api.dto.category.CategoryDto;
import com.servantin.api.service.AuthService;
import com.servantin.api.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("GET /api/categories should return all categories")
    void getAllCategories_success() throws Exception {
        // Given
        List<CategoryDto> categories = List.of(
                CategoryDto.builder()
                        .id(UUID.randomUUID())
                        .slug("babysitting")
                        .name("Babysitting & Nanny")
                        .description("Garde d'enfants")
                        .icon("baby")
                        .sortOrder(1)
                        .build(),
                CategoryDto.builder()
                        .id(UUID.randomUUID())
                        .slug("home-support")
                        .name("Home Support & Handyman")
                        .description("Bricolage")
                        .icon("wrench")
                        .sortOrder(2)
                        .build());

        when(categoryService.getAllCategories()).thenReturn(categories);

        // When/Then
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("babysitting"))
                .andExpect(jsonPath("$[1].slug").value("home-support"));
    }

    @Test
    @DisplayName("GET /api/categories/{slug} should return category by slug")
    void getCategoryBySlug_success() throws Exception {
        // Given
        CategoryDto category = CategoryDto.builder()
                .id(UUID.randomUUID())
                .slug("babysitting")
                .name("Babysitting & Nanny")
                .description("Garde d'enfants à domicile")
                .icon("baby")
                .sortOrder(1)
                .build();

        when(categoryService.getCategoryBySlug("babysitting")).thenReturn(category);

        // When/Then
        mockMvc.perform(get("/api/categories/babysitting"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.slug").value("babysitting"))
                .andExpect(jsonPath("$.name").value("Babysitting & Nanny"));
    }

    @Test
    @DisplayName("GET /api/categories/{slug} should return 404 for unknown slug")
    void getCategoryBySlug_notFound() throws Exception {
        // Given
        when(categoryService.getCategoryBySlug("unknown"))
                .thenThrow(new RuntimeException("Category not found: unknown"));

        // When/Then
        mockMvc.perform(get("/api/categories/unknown"))
                .andExpect(status().isNotFound());
    }
}
