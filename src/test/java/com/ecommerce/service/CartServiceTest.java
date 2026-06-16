package com.ecommerce.service;

import com.ecommerce.dto.request.CartItemRequest;
import com.ecommerce.dto.response.CartResponse;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Product product;
    private Cart cart;
    private CartItemRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Test User").build();

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

        request = CartItemRequest.builder()
                .productId(1L)
                .quantity(2)
                .build();
    }

    @Test
    void addItemToCart_ShouldAddNewItem_WhenProductNotInCart() {
        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(1L, request);

        assertNotNull(response);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItemToCart_ShouldIncrementQuantity_WhenProductAlreadyInCart() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(existingItem);

        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(1L, request);

        assertNotNull(response);
        assertEquals(3, existingItem.getQuantity());
    }

    @Test
    void addItemToCart_ShouldThrowException_WhenInsufficientStock() {
        product.setStock(1);
        request.setQuantity(2);

        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class,
                () -> cartService.addItemToCart(1L, request));
    }

    @Test
    void addItemToCart_ShouldThrowException_WhenProductNotFound() {
        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.addItemToCart(1L, request));
    }

    @Test
    void addItemToCart_ShouldCreateNewCart_WhenUserHasNoCart() {
        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(1L, request);

        assertNotNull(response);
        verify(cartRepository, times(2)).save(any(Cart.class));
    }

    @Test
    void updateItemQuantity_ShouldUpdate_WhenPositiveQuantity() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(existingItem);

        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.updateItemQuantity(1L, 1L, 3);

        assertNotNull(response);
        assertEquals(3, existingItem.getQuantity());
    }

    @Test
    void updateItemQuantity_ShouldRemoveItem_WhenQuantityZeroOrNegative() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(existingItem);

        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.updateItemQuantity(1L, 1L, 0);

        assertNotNull(response);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void updateItemQuantity_ShouldThrowException_WhenProductNotInCart() {
        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.updateItemQuantity(1L, 99L, 3));
    }

    @Test
    void removeItemFromCart_ShouldRemoveItem_WhenExists() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(existingItem);

        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponse response = cartService.removeItemFromCart(1L, 1L);

        assertNotNull(response);
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void clearCart_ShouldRemoveAllItems() {
        CartItem existingItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .product(product)
                .quantity(1)
                .build();
        cart.getItems().add(existingItem);

        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(1L);

        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void getCart_ShouldReturnCartResponse() {
        when(cartRepository.findWithItemsByUserId(1L)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(1L);

        assertNotNull(response);
        assertEquals(0, response.getTotalItems());
    }
}
