package com.ecommerce.service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private Category parentCategory;
    private Category subCategory;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic items")
                .build();

        category = Category.builder()
                .id(2L)
                .name("Mobile Phones")
                .description("Mobile phones and accessories")
                .parent(parentCategory)
                .build();

        subCategory = Category.builder()
                .id(3L)
                .name("Smartphones")
                .description("Smartphones")
                .parent(category)
                .build();
    }

    @Test
    void createCategory_ShouldReturnCategory_WhenSuccessful() {
        when(categoryRepository.existsByName("Mobile Phones")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.createCategory("Mobile Phones", "Mobile phones", null);

        assertNotNull(result);
        assertEquals("Mobile Phones", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_WithParent_ShouldSetParent() {
        when(categoryRepository.existsByName("Mobile Phones")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.createCategory("Mobile Phones", "Mobile phones", 1L);

        assertNotNull(result);
        assertEquals(parentCategory, result.getParent());
    }

    @Test
    void createCategory_ShouldThrowException_WhenNameExists() {
        when(categoryRepository.existsByName("Mobile Phones")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> categoryService.createCategory("Mobile Phones", "desc", null));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_ShouldThrowException_WhenParentNotFound() {
        when(categoryRepository.existsByName("Mobile Phones")).thenReturn(false);
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.createCategory("Mobile Phones", "desc", 99L));
    }

    @Test
    void getCategoryById_ShouldReturnCategory_WhenFound() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryById(2L);

        assertNotNull(result);
        assertEquals("Mobile Phones", result.getName());
    }

    @Test
    void getCategoryById_ShouldThrowException_WhenNotFound() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getCategoryById(99L));
    }

    @Test
    void getAllTopLevelCategories_ShouldReturnOnlyParentCategories() {
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(parentCategory));

        List<Category> result = categoryService.getAllTopLevelCategories();

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0).getName());
    }

    @Test
    void getSubCategories_ShouldReturnChildren() {
        when(categoryRepository.findByParentId(2L)).thenReturn(List.of(subCategory));

        List<Category> result = categoryService.getSubCategories(2L);

        assertEquals(1, result.size());
        assertEquals("Smartphones", result.get(0).getName());
    }

    @Test
    void updateCategory_ShouldUpdateName() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("New Name")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.updateCategory(2L, "New Name", null);

        assertEquals("New Name", result.getName());
    }

    @Test
    void updateCategory_ShouldUpdateDescription() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        Category result = categoryService.updateCategory(2L, null, "New description");

        assertEquals("New description", result.getDescription());
    }

    @Test
    void updateCategory_ShouldThrowException_WhenNameAlreadyTaken() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName("Taken Name")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> categoryService.updateCategory(2L, "Taken Name", null));
    }

    @Test
    void deleteCategory_ShouldDelete() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(2L);

        verify(categoryRepository).delete(category);
    }
}
