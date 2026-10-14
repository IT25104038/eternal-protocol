package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.entity.CartItem;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.CartItemRepository;
import com.eternalprotocol.api.repository.CustomerRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for a logged-in customer's saved cart.
 */
@ExtendWith(MockitoExtension.class)
class CartTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartService cartService;

    private Customer customer;
    private Product hoodie;

    @BeforeEach
    void setUp() {
        customer = new Customer("Nimal", "0771234567", "Colombo");
        customer.setId(1L);

        hoodie = new Product("HOOD-01", "Team Hoodie", ProductCategory.MENS, "Hoodies",
                new BigDecimal("8500.00"), "Black", "M", 10, null, "A hoodie");
        hoodie.setId(10L);
    }

    @Test
    void addToCart_newItem_createsCartLineWithRequestedQuantity() {
        // Given: the cart does not contain this hoodie yet
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(hoodie));
        when(cartItemRepository.findByCustomerIdAndProductIdAndSizeAndColour(1L, 10L, "M", "Black"))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(call -> call.getArgument(0));

        // When
        CartItemDto result = cartService.addToCart(1L, new CartItemRequestDto(10L, "M", "Black", 2));

        // Then
        assertEquals("Team Hoodie", result.productName());
        assertEquals(2, result.quantity());
    }

    @Test
    void addToCart_sameItemAgain_increasesQuantityInsteadOfAddingDuplicate() {
        // Given: the cart already has 2 of this exact hoodie (same product, size and colour)
        CartItem existingLine = new CartItem(customer, hoodie, "M", "Black", 2);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(hoodie));
        when(cartItemRepository.findByCustomerIdAndProductIdAndSizeAndColour(1L, 10L, "M", "Black"))
                .thenReturn(Optional.of(existingLine));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(call -> call.getArgument(0));

        // When: the customer adds 3 more
        CartItemDto result = cartService.addToCart(1L, new CartItemRequestDto(10L, "M", "Black", 3));

        // Then: 2 + 3 = 5 on the same line
        assertEquals(5, result.quantity());
    }

    @Test
    void updateQuantity_setsTheExactQuantity() {
        // Given: a cart line with quantity 2
        CartItem line = new CartItem(customer, hoodie, "M", "Black", 2);
        when(cartItemRepository.findByIdAndCustomerId(5L, 1L)).thenReturn(Optional.of(line));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(call -> call.getArgument(0));

        // When: the customer changes it to 7
        CartItemDto result = cartService.updateQuantity(1L, 5L, 7);

        // Then: it is exactly 7, not 2 + 7
        assertEquals(7, result.quantity());
    }

    @Test
    void removeFromCart_whenLineIsNotThisCustomers_throwsAndDeletesNothing() {
        // Given: cart line 5 does not belong to customer 1 (or does not exist)
        when(cartItemRepository.findByIdAndCustomerId(5L, 1L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> cartService.removeFromCart(1L, 5L));
        verify(cartItemRepository, never()).delete(any(CartItem.class));
    }
}
