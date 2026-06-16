package com.ecommerce.controller;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartItemResponse;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.service.CartService;
import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.support.WithMockPrincipal;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private CartResponse createCartResponse() {
        CartItemResponse item = CartItemResponse.builder()
                .cartItemId(1L)
                .productId(1L)
                .productName("Laptop")
                .unitPrice(new BigDecimal("999.99"))
                .quantity(2)
                .subtotal(new BigDecimal("1999.98"))
                .build();

        return CartResponse.builder()
                .cartId(1L)
                .items(List.of(item))
                .totalAmount(new BigDecimal("1999.98"))
                .totalItems(1)
                .build();
    }

    @Test
    @WithMockPrincipal
    void getCart_ShouldReturnCart() throws Exception {
        when(cartService.getCart(anyLong())).thenReturn(createCartResponse());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @WithMockPrincipal
    void addItem_ShouldReturnCart() throws Exception {
        when(cartService.addItemToCart(anyLong(), any(CartItemRequest.class)))
                .thenReturn(createCartResponse());

        CartItemRequest request = CartItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"));
    }

    @Test
    @WithMockPrincipal
    void addItem_ShouldReturn400_WhenQuantityInvalid() throws Exception {
        CartItemRequest request = CartItemRequest.builder()
                .productId(1L)
                .quantity(0)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockPrincipal
    void updateItem_ShouldReturnCart() throws Exception {
        when(cartService.updateItemQuantity(anyLong(), anyLong(), anyInt()))
                .thenReturn(createCartResponse());

        mockMvc.perform(put("/api/cart/items/1")
                        .param("quantity", "3"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal
    void removeItem_ShouldReturnCart() throws Exception {
        when(cartService.removeItemFromCart(anyLong(), anyLong()))
                .thenReturn(createCartResponse());

        mockMvc.perform(delete("/api/cart/items/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockPrincipal
    void clearCart_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isNoContent());
    }
}
