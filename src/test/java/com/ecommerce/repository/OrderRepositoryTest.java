package com.ecommerce.repository;

import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private User user;
    private Order order;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Test User")
                .email("user@example.com")
                .password("password")
                .role(Role.CUSTOMER)
                .build();
        entityManager.persist(user);

        order = Order.builder()
                .user(user)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .shippingAddress("123 Main St")
                .build();
        entityManager.persist(order);
        entityManager.flush();
    }

    @Test
    void findByUserId_ShouldReturnUserOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> result = orderRepository.findByUserId(user.getId(), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(OrderStatus.PENDING, result.getContent().get(0).getOrderStatus());
    }

    @Test
    void findByUserId_ShouldReturnEmpty_WhenNoOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> result = orderRepository.findByUserId(999L, pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByOrderStatus_ShouldReturnOrdersWithStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> pendingOrders = orderRepository.findByOrderStatus(
                OrderStatus.PENDING, pageable);
        Page<Order> confirmedOrders = orderRepository.findByOrderStatus(
                OrderStatus.CONFIRMED, pageable);

        assertEquals(1, pendingOrders.getTotalElements());
        assertTrue(confirmedOrders.isEmpty());
    }

    @Test
    void findStaleOrders_ShouldReturnOrdersBeforeDate() {
        Order staleOrder = Order.builder()
                .user(user)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(new BigDecimal("99.99"))
                .shippingAddress("456 Oak St")
                .build();
        entityManager.persist(staleOrder);
        entityManager.flush();

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Order> staleOrders = orderRepository.findStaleOrders(
                OrderStatus.PENDING, yesterday);

        assertTrue(staleOrders.isEmpty());

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        List<Order> staleOrdersAfter = orderRepository.findStaleOrders(
                OrderStatus.PENDING, tomorrow);

        assertEquals(2, staleOrdersAfter.size());
    }

    @Test
    void countByUserAndStatus_ShouldReturnCount() {
        long count = orderRepository.countByUserAndStatus(
                user.getId(), OrderStatus.PENDING);

        assertEquals(1, count);
    }

    @Test
    void countByUserAndStatus_ShouldReturnZero_WhenNoMatch() {
        long count = orderRepository.countByUserAndStatus(
                user.getId(), OrderStatus.CONFIRMED);

        assertEquals(0, count);
    }

    @Test
    void getTotalRevenueBetween_ShouldReturnSum() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        Double revenue = orderRepository.getTotalRevenueBetween(
                OrderStatus.PENDING, start, end);

        assertNotNull(revenue);
        assertEquals(1999.98, revenue, 0.01);
    }

    @Test
    void getTotalRevenueBetween_ShouldReturnZero_WhenNoOrders() {
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        Double revenue = orderRepository.getTotalRevenueBetween(
                OrderStatus.PENDING, start, end);

        assertEquals(0.0, revenue, 0.01);
    }
}
