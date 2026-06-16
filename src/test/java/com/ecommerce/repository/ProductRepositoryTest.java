package com.ecommerce.repository;

import com.ecommerce.enums.Role;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Category category;
    private User seller;
    private Product product;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .name("Electronics")
                .build();
        entityManager.persist(category);

        seller = User.builder()
                .name("Seller")
                .email("seller@example.com")
                .password("password")
                .role(Role.SELLER)
                .build();
        entityManager.persist(seller);

        product = Product.builder()
                .name("Gaming Laptop")
                .description("High performance gaming laptop")
                .price(new BigDecimal("1499.99"))
                .stock(10)
                .imageUrl("laptop.jpg")
                .category(category)
                .seller(seller)
                .build();
        entityManager.persist(product);
        entityManager.flush();
    }

    @Test
    void findByCategoryId_ShouldReturnProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByCategoryId(category.getId(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Gaming Laptop", result.getContent().get(0).getName());
    }

    @Test
    void findByCategoryId_ShouldReturnEmpty_WhenNoProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByCategoryId(999L, pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByNameContainingIgnoreCase_ShouldFindByName() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByNameContainingIgnoreCase(
                "gaming", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByNameContainingIgnoreCase_ShouldBeCaseInsensitive() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByNameContainingIgnoreCase(
                "GAMING", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchByKeyword_ShouldSearchByNameAndDescription() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.searchByKeyword("laptop", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchByKeyword_ShouldSearchInDescription() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.searchByKeyword("performance", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findBySellerId_ShouldReturnSellerProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findBySellerId(seller.getId(), pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findLowStockProducts_ShouldReturnProductsBelowThreshold() {
        List<Product> lowStock = productRepository.findLowStockProducts(5);
        assertTrue(lowStock.isEmpty());

        product.setStock(3);
        entityManager.persist(product);
        entityManager.flush();

        List<Product> lowStockAfter = productRepository.findLowStockProducts(5);
        assertEquals(1, lowStockAfter.size());
    }

    @Test
    void findLowStockProducts_ShouldOrderByStockAsc() {
        Product product2 = Product.builder()
                .name("Mouse")
                .description("Wireless mouse")
                .price(new BigDecimal("49.99"))
                .stock(1)
                .category(category)
                .seller(seller)
                .build();
        entityManager.persist(product2);

        product.setStock(3);
        entityManager.persist(product);
        entityManager.flush();

        List<Product> lowStock = productRepository.findLowStockProducts(5);
        assertEquals(2, lowStock.size());
        assertTrue(lowStock.get(0).getStock() <= lowStock.get(1).getStock());
    }
}
