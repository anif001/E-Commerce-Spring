package com.ecommerce.service;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Product product;
    private Category category;
    private User seller;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        seller = User.builder()
                .id(2L)
                .name("Test Seller")
                .build();

        productRequest = ProductRequest.builder()
                .name("Laptop")
                .description("High performance laptop")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .imageUrl("laptop.jpg")
                .categoryId(1L)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("High performance laptop")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .imageUrl("laptop.jpg")
                .category(category)
                .seller(seller)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createProduct_ShouldReturnProductResponse_WhenSuccessful() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest, 2L);

        assertNotNull(response);
        assertEquals("Laptop", response.getName());
        assertEquals(new BigDecimal("999.99"), response.getPrice());
        assertEquals(10, response.getStock());
        assertEquals("Electronics", response.getCategoryName());
        assertEquals(1L, response.getCategoryId());
        assertEquals("Test Seller", response.getSellerName());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_ShouldThrowException_WhenCategoryNotFound() {
        when(categoryRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.createProduct(productRequest, 2L));

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_ShouldThrowException_WhenSellerNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.createProduct(productRequest, 2L));

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_ShouldReturnProductResponse_WhenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals("Laptop", response.getName());
    }

    @Test
    void getProductById_ShouldThrowException_WhenNotFound() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById(99L));
    }

    @Test
    void getProductsByCategory_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findByCategoryId(1L, pageable)).thenReturn(productPage);

        Page<ProductResponse> result = productService.getProductsByCategory(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getName());
    }

    @Test
    void searchProducts_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.searchByKeyword("laptop", pageable)).thenReturn(productPage);

        Page<ProductResponse> result = productService.searchProducts("laptop", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateProduct_ShouldUpdate_WhenAuthorized() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductRequest updateRequest = ProductRequest.builder()
                .name("Gaming Laptop")
                .description("Updated description")
                .price(new BigDecimal("1299.99"))
                .stock(5)
                .imageUrl("gaming.jpg")
                .categoryId(1L)
                .build();

        ProductResponse response = productService.updateProduct(1L, updateRequest, 2L);

        assertNotNull(response);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldThrowException_WhenUnauthorized() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class,
                () -> productService.updateProduct(1L, productRequest, 99L));
    }

    @Test
    void deleteProduct_ShouldDelete_WhenAuthorized() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L, 2L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_ShouldThrowException_WhenUnauthorized() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class,
                () -> productService.deleteProduct(1L, 99L));

        verify(productRepository, never()).delete(any());
    }

    @Test
    void getSellerProducts_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findBySellerId(2L, pageable)).thenReturn(productPage);

        Page<ProductResponse> result = productService.getSellerProducts(2L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
