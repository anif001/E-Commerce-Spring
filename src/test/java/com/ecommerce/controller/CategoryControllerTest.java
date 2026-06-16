package com.ecommerce.controller;

import com.ecommerce.model.Category;
import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.service.CategoryService;
import com.ecommerce.support.WithMockPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private Category createCategory(Long id, String name, String description) {
        return Category.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
    }

    @Test
    void getAllCategories_ShouldReturnList() throws Exception {
        when(categoryService.getAllTopLevelCategories())
                .thenReturn(List.of(
                        createCategory(1L, "Electronics", "Electronic items"),
                        createCategory(2L, "Books", "Books")
                ));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getCategory_ShouldReturnCategory() throws Exception {
        when(categoryService.getCategoryById(1L))
                .thenReturn(createCategory(1L, "Electronics", "Electronic items"));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void getSubCategories_ShouldReturnList() throws Exception {
        when(categoryService.getSubCategories(1L))
                .thenReturn(List.of(
                        createCategory(3L, "Mobile Phones", "Mobile phones")
                ));

        mockMvc.perform(get("/api/categories/1/subcategories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Mobile Phones"));
    }

    @Test
    @WithMockPrincipal(role = "ADMIN")
    void createCategory_ShouldReturn201() throws Exception {
        Category category = createCategory(1L, "Electronics", "Electronic items");
        when(categoryService.createCategory(anyString(), anyString(), any()))
                .thenReturn(category);

        Map<String, Object> request = Map.of(
                "name", "Electronics",
                "description", "Electronic items"
        );

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @Disabled("Method security (@PreAuthorize) not enforced in @WebMvcTest")
    void createCategory_ShouldReturn403_WhenNotAdmin() throws Exception {
        Map<String, Object> request = Map.of("name", "Electronics");

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(role = "ADMIN")
    void updateCategory_ShouldReturn200() throws Exception {
        Category category = createCategory(1L, "Updated Name", "Updated desc");
        when(categoryService.updateCategory(anyLong(), anyString(), any()))
                .thenReturn(category);

        Map<String, String> request = Map.of("name", "Updated Name");

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockPrincipal(role = "ADMIN")
    void deleteCategory_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}
