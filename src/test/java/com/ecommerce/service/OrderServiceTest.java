package com.ecommerce.service;

import com.ecommerce.dto.response.OrderResponse;
import com.ecommerce.enums.OrderStatus;
import com.ecommerce.enums.PaymentStatus;
import com.ecommerce.enums.Role;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.*;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Test User").email("test@example.com").build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .build();

        cart = Cart.builder()
                .id(1L)
                .user(user)
                .items(new ArrayList<>())
                .build();

        cartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(2)
                .build();
        cart.getItems().add(cartItem);

        orderItem = OrderItem.builder()
                .id(1L)
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("999.99"))
                .subtotal(new BigDecimal("1999.98"))
                .build();

        order = Order.builder()
                .id(1L)
                .user(user)
                .orderStatus(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(new BigDecimal("1999.98"))
                .shippingAddress("123 Main St")
                .orderItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build();
        order.getOrderItems().add(orderItem);
    }

    @Test
    void placeOrder_ShouldCreateOrder_WhenCartHasItems() {
        when(cartService.getOrCreateCart(1L)).thenReturn(cart);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        OrderResponse response = orderService.placeOrder(1L, "123 Main St");

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING, response.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals(new BigDecimal("1999.98"), response.getTotalAmount());
        assertEquals("123 Main St", response.getShippingAddress());

        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void placeOrder_ShouldThrowException_WhenCartEmpty() {
        cart.getItems().clear();
        when(cartService.getOrCreateCart(1L)).thenReturn(cart);

        assertThrows(BadRequestException.class,
                () -> orderService.placeOrder(1L, "123 Main St"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_ShouldThrowException_WhenInsufficientStock() {
        product.setStock(1);

        when(cartService.getOrCreateCart(1L)).thenReturn(cart);

        assertThrows(BadRequestException.class,
                () -> orderService.placeOrder(1L, "123 Main St"));
    }

    @Test
    void placeOrder_ShouldDeductStock() {
        when(cartService.getOrCreateCart(1L)).thenReturn(cart);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        orderService.placeOrder(1L, "123 Main St");

        assertEquals(8, product.getStock());
        verify(productRepository).save(product);
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenAuthorized() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getOrderById_ShouldThrowException_WhenNotAuthorized() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.getOrderById(1L, 99L));
    }

    @Test
    void getOrderById_ShouldThrowException_WhenNotFound() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(99L, 1L));
    }

    @Test
    void getUserOrders_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findByUserId(1L, pageable)).thenReturn(orderPage);

        Page<OrderResponse> result = orderService.getUserOrders(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void cancelOrder_ShouldCancel_WhenPending() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.cancelOrder(1L, 1L);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.getOrderStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_ShouldRestoreStock() {
        orderItem.setQuantity(2);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.cancelOrder(1L, 1L);

        assertEquals(12, product.getStock());
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenNotAuthorized() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.cancelOrder(1L, 99L));
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenAlreadyShipped() {
        order.setOrderStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.cancelOrder(1L, 1L));
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenAlreadyDelivered() {
        order.setOrderStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class,
                () -> orderService.cancelOrder(1L, 1L));
    }

    @Test
    void getAllOrders_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        when(orderRepository.findAll(pageable)).thenReturn(orderPage);

        Page<OrderResponse> result = orderService.getAllOrders(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateOrderStatus_ShouldUpdate() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getOrderStatus());
    }
}
