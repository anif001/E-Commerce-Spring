package com.ecommerce.controller;

import com.ecommerce.dto.request.OrderRequest;
import com.ecommerce.dto.response.OrderItemResponse;
import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.service.OrderService;
import com.ecommerce.support.WithMockPrincipal;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private OrderResponse createOrderResponse() {
        OrderItemResponse item = OrderItemResponse.builder()
                .productId(1L)
                .productName("Laptop")
                .quantity(2)
                .unitPrice(new BigDecimal("999.99"))
                .subtotal(new BigDecimal("1999.98"))
                .build();

        return OrderResponse.builder()
                .id(1L)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .shippingAddress("123 Main St")
                .items(List.of(item))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockPrincipal
    void placeOrder_ShouldReturn201() throws Exception {
        when(orderService.placeOrder(anyLong(), anyString()))
                .thenReturn(createOrderResponse());

        OrderRequest request = OrderRequest.builder()
                .shippingAddress("123 Main St")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"));
    }

    @Test
    @WithMockPrincipal
    void placeOrder_ShouldReturn400_WhenAddressMissing() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .shippingAddress("")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockPrincipal
    void getMyOrders_ShouldReturnPage() throws Exception {
        Page<OrderResponse> page = new PageImpl<>(List.of(createOrderResponse()));
        when(orderService.getUserOrders(anyLong(), any(PageRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderStatus").value("PENDING"));
    }

    @Test
    @WithMockPrincipal
    void getOrder_ShouldReturnOrder() throws Exception {
        when(orderService.getOrderById(anyLong(), anyLong()))
                .thenReturn(createOrderResponse());

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1999.98));
    }

    @Test
    @WithMockPrincipal
    void cancelOrder_ShouldReturnUpdatedOrder() throws Exception {
        OrderResponse cancelled = createOrderResponse();
        cancelled.setOrderStatus(OrderStatus.CANCELLED);
        when(orderService.cancelOrder(anyLong(), anyLong())).thenReturn(cancelled);

        mockMvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    }

    @Test
    @WithMockPrincipal(role = "ADMIN")
    void getAllOrders_ShouldReturnAll_WhenAdmin() throws Exception {
        Page<OrderResponse> page = new PageImpl<>(List.of(createOrderResponse()));
        when(orderService.getAllOrders(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/orders/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0]").exists());
    }

    @Test
    @Disabled("Method security (@PreAuthorize) not enforced in @WebMvcTest")
    void getAllOrders_ShouldReturn403_WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/orders/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockPrincipal(role = "ADMIN")
    void updateStatus_ShouldReturnUpdatedOrder() throws Exception {
        OrderResponse confirmed = createOrderResponse();
        confirmed.setOrderStatus(OrderStatus.CONFIRMED);
        when(orderService.updateOrderStatus(anyLong(), any(OrderStatus.class)))
                .thenReturn(confirmed);

        String requestBody = "{\"status\": \"CONFIRMED\"}";

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));
    }
}
