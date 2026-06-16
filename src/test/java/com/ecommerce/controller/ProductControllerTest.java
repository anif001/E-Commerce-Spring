package com.ecommerce.controller;

import com.ecommerce.dto.request.ProductRequest;
import com.ecommerce.dto.response.ProductResponse;
import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.support.WithMockPrincipal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private ProductResponse createProductResponse() {
        return ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .description("High performance laptop")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .imageUrl("laptop.jpg")
                .categoryName("Electronics")
                .categoryId(1L)
                .sellerName("Seller")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void searchProducts_ShouldReturnResults_WhenQueryProvided() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(createProductResponse()));
        when(productService.searchProducts(anyString(), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/products")
                        .param("q", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.content[0].price").value(999.99));
    }

    @Test
    void searchProducts_ShouldReturnEmpty_WhenNoQuery() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test
    void getProduct_ShouldReturnProduct_WhenFound() throws Exception {
        when(productService.getProductById(1L)).thenReturn(createProductResponse());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getByCategory_ShouldReturnProducts() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(createProductResponse()));
        when(productService.getProductsByCategory(anyLong(), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/products/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoryId").value(1));
    }

    @Test
    @WithMockPrincipal(role = "SELLER")
    void createProduct_ShouldReturn201_WhenSeller() throws Exception {
        when(productService.createProduct(any(ProductRequest.class), anyLong()))
                .thenReturn(createProductResponse());

        ProductRequest request = ProductRequest.builder()
                .name("Laptop")
                .description("High performance laptop")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .imageUrl("laptop.jpg")
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @Disabled("Method security (@PreAuthorize) not enforced in @WebMvcTest")
    void createProduct_ShouldReturn403_WhenNotSeller() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Laptop")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .categoryId(1L)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(role = "SELLER")
    void createProduct_ShouldReturn400_WhenInvalidData() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("")
                .price(new BigDecimal("-1"))
                .stock(-1)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockPrincipal(role = "SELLER")
    void updateProduct_ShouldReturn200_WhenAuthorized() throws Exception {
        when(productService.updateProduct(anyLong(), any(ProductRequest.class), anyLong()))
                .thenReturn(createProductResponse());

        ProductRequest request = ProductRequest.builder()
                .name("Updated Laptop")
                .price(new BigDecimal("1299.99"))
                .stock(5)
                .categoryId(1L)
                .build();

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal(role = "SELLER")
    void deleteProduct_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
