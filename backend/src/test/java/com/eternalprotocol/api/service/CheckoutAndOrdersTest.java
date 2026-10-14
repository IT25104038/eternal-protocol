package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.OrderItemRequestDto;
import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.OrderResponseDto;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.CommissionStatus;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.entity.OrderStatus;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.entity.ProductStatus;
import com.eternalprotocol.api.exception.InsufficientStockException;
import com.eternalprotocol.api.exception.ProductNotAvailableException;
import com.eternalprotocol.api.factory.CustomerFactory;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import com.eternalprotocol.api.repository.OrderRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for checking out (stock, athlete discount, commission) and for
 * confirming a payment.
 *
 * Every product here costs 100.00, so the maths is easy to follow.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutAndOrdersTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerFactory customerFactory;
    @Mock
    private AthleteRepository athleteRepository;
    @Mock
    private CommissionRepository commissionRepository;
    @Mock
    private CartService cartService;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product hoodie;

    @BeforeEach
    void setUp() {
        customer = new Customer("Nimal", "0771234567", "Colombo");

        hoodie = new Product("HOOD-01", "Team Hoodie", ProductCategory.MENS, "Hoodies",
                new BigDecimal("100.00"), "Black", "M", 10, null, "A hoodie");
        hoodie.setId(1L);
    }

    /** A checkout request for `quantity` hoodies, with an optional athlete code. */
    private OrderRequestDto checkoutRequest(int quantity, String athleteCode) {
        return new OrderRequestDto("Nimal", "0771234567", "Colombo",
                List.of(new OrderItemRequestDto(1L, "M", "Black", quantity)), athleteCode);
    }

    @Test
    void createOrder_withValidAthleteCode_appliesDiscountReducesStockAndRecordsCommission() {
        // Given: athlete SARA10 gives a 10% discount and earns a 10% commission
        Athlete athlete = new Athlete("Sara", "sara@test.com", "HASHED", "SARA10", new BigDecimal("10.00"), true);
        OrderRequestDto request = checkoutRequest(2, "SARA10");

        when(productRepository.findById(1L)).thenReturn(Optional.of(hoodie));
        when(athleteRepository.findByAthleteCodeIgnoreCase("SARA10")).thenReturn(Optional.of(athlete));
        when(customerFactory.resolveForCheckout(null, request)).thenReturn(customer);
        when(orderRepository.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        when(paymentService.initiatePayment(any(Order.class), any(BigDecimal.class))).thenReturn("http://pay.test/1");

        // When: a guest buys 2 hoodies (2 x 100.00 = 200.00)
        OrderResponseDto response = orderService.createOrder(request, null);

        // Then: 10% off 200.00 is 20.00, so the customer pays 180.00
        assertEquals(new BigDecimal("180.00"), response.totalAmount());
        assertEquals("http://pay.test/1", response.paymentUrl());

        // Stock goes from 10 down to 8
        assertEquals(8, hoodie.getStockQty());

        // The athlete earns a commission of 20.00
        ArgumentCaptor<Commission> commission = ArgumentCaptor.forClass(Commission.class);
        verify(commissionRepository).save(commission.capture());
        assertEquals(new BigDecimal("20.00"), commission.getValue().getCommissionAmount());
    }

    @Test
    void createOrder_withoutAthleteCode_hasNoDiscountAndNoCommission() {
        // Given: a checkout with no athlete code
        OrderRequestDto request = checkoutRequest(1, null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(hoodie));
        when(customerFactory.resolveForCheckout(null, request)).thenReturn(customer);
        when(orderRepository.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        when(paymentService.initiatePayment(any(Order.class), any(BigDecimal.class))).thenReturn("http://pay.test/1");

        // When
        OrderResponseDto response = orderService.createOrder(request, null);

        // Then: full price, and nobody earns a commission
        assertEquals(new BigDecimal("100.00"), response.totalAmount());
        verifyNoInteractions(athleteRepository);
        verify(commissionRepository, never()).save(any(Commission.class));
    }

    @Test
    void createOrder_whenNotEnoughStock_throwsAndSavesNoOrder() {
        // Given: only 1 hoodie left, but the customer wants 5
        hoodie.setStockQty(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(hoodie));

        // When / Then
        assertThrows(InsufficientStockException.class,
                () -> orderService.createOrder(checkoutRequest(5, null), null));
        verify(orderRepository, never()).save(any(Order.class));
        assertEquals(1, hoodie.getStockQty());
    }

    @Test
    void createOrder_forComingSoonProduct_throwsNotAvailable() {
        // Given: the hoodie has not launched yet
        hoodie.setStatus(ProductStatus.COMING_SOON);
        when(productRepository.findById(1L)).thenReturn(Optional.of(hoodie));

        // When / Then
        assertThrows(ProductNotAvailableException.class,
                () -> orderService.createOrder(checkoutRequest(1, null), null));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void confirmPayment_marksOrderPaidAndTheAthletesCommissionEarned() {
        // Given: an unpaid order that has a commission waiting on the payment
        Athlete athlete = new Athlete("Sara", "sara@test.com", "HASHED", "SARA10", new BigDecimal("10.00"), true);
        Order order = new Order(customer, new BigDecimal("200.00"), new BigDecimal("20.00"), "SARA10");
        order.setId(1L);
        Commission commission = new Commission(athlete, order, new BigDecimal("20.00"));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(commissionRepository.findByOrderId(1L)).thenReturn(Optional.of(commission));

        // When: PayHere tells us the payment went through
        orderService.confirmPayment(1L);

        // Then
        assertEquals(OrderStatus.PAID, order.getPaymentStatus());
        assertEquals(CommissionStatus.EARNED, commission.getCommissionStatus());
    }
}
