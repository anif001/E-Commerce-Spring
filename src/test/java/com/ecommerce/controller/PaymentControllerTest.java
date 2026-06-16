package com.ecommerce.controller;

import com.ecommerce.security.CustomUserDetailsService;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser
    void createRazorpayOrder_ShouldReturnOrderId() throws Exception {
        when(paymentService.createRazorpayOrder(anyLong())).thenReturn("razorpay_order_123");

        mockMvc.perform(post("/api/payments/create-order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razorpayOrderId").value("razorpay_order_123"))
                .andExpect(jsonPath("$.status").value("created"));
    }

    @Test
    @WithMockUser
    void verifyPayment_ShouldReturnVerified() throws Exception {
        doNothing().when(paymentService).verifyPayment(anyString(), anyString(), anyString());

        Map<String, String> paymentData = Map.of(
                "razorpayOrderId", "order_123",
                "razorpayPaymentId", "pay_123",
                "signature", "sig_123"
        );

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("verified"));
    }
}
