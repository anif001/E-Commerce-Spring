package com.ecommerce.service;

import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.model.Order;
import com.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(1L)
                .totalAmount(new BigDecimal("1999.98"))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test_key");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "test_secret");
    }

    @Test
    void createRazorpayOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> paymentService.createRazorpayOrder(99L));
    }

    @Test
    void verifyPayment_ShouldUpdateStatus_WhenSignatureValid() {
        when(orderRepository.findAll()).thenReturn(List.of(order));

        order.setRazorpayOrderId("razorpay_order_1");

        String validSignature = paymentService.HmacSHA256(
                "razorpay_order_1|razorpay_payment_1", "test_secret");

        paymentService.verifyPayment(
                "razorpay_order_1", "razorpay_payment_1", validSignature);

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals("razorpay_payment_1", order.getRazorpayPaymentId());
        verify(orderRepository).save(order);
    }

    @Test
    void verifyPayment_ShouldThrowException_WhenSignatureInvalid() {
        order.setRazorpayOrderId("razorpay_order_1");

        assertThrows(RuntimeException.class,
                () -> paymentService.verifyPayment(
                        "razorpay_order_1", "razorpay_payment_1", "invalid_signature"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void verifyPayment_ShouldThrowException_WhenOrderNotFound() {
        order.setRazorpayOrderId("razorpay_order_1");
        when(orderRepository.findAll()).thenReturn(List.of(order));

        String validSignature = paymentService.HmacSHA256(
                "wrong_order|razorpay_payment_1", "test_secret");

        assertThrows(RuntimeException.class,
                () -> paymentService.verifyPayment(
                        "wrong_order", "razorpay_payment_1", validSignature));
    }
}
