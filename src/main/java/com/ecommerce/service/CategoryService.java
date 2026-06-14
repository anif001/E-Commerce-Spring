package com.ecommerce.service;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(String name, String description, Long parentId) {
        if (categoryRepository.existsByName(name)) {
            throw new BadRequestException("Category already exists: " + name);
        }

        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();

        if (parentId != null) {
            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent category not found: " + parentId));
            category.setParent(parent);
        }

        return categoryRepository.save(category);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found: " + id));
    }

    public List<Category> getAllTopLevelCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public List<Category> getSubCategories(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    @Transactional
    public Category updateCategory(Long id, String name, String description) {
        Category category = getCategoryById(id);

        if (name != null && !name.equals(category.getName())) {
            if (categoryRepository.existsByName(name)) {
                throw new BadRequestException("Category name already taken: " + name);
            }
            category.setName(name);
        }
        if (description != null) {
            category.setDescription(description);
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
