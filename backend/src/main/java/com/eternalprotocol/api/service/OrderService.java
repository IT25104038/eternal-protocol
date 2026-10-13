package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.*;
import com.eternalprotocol.api.entity.*;
import com.eternalprotocol.api.exception.InsufficientStockException;
import com.eternalprotocol.api.exception.ProductNotAvailableException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.factory.CustomerFactory;
import com.eternalprotocol.api.referral.AppliedAthleteReferral;
import com.eternalprotocol.api.referral.AthleteReferral;
import com.eternalprotocol.api.referral.NoAthleteReferral;
import com.eternalprotocol.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing order creation, PayHere payment confirmation, and order history retrieval
 * <p>
 * Read operations are annotated with {@code @Transactional(readOnly = true)} to maintain an 
 * active database session during DTO mapping. With {@code open-in-view: false}, this ensures 
 * lazy associations {@code order.getItems()} are safely initialized, preventing 
 * {@code LazyInitializationException}s during real requests
 */

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerFactory customerFactory;
    private final AthleteRepository athleteRepository;
    private final CommissionRepository commissionRepository;
    private final CartService cartService;
    private final PaymentService paymentService;

    /**
     * @param orderRepository  order repository
     * @param productRepository  product repository
     * @param customerFactory   customer factory
     * @param athleteRepository athlete repository
     * @param commissionRepository commission repository
     * @param cartService  cart service
     * @param paymentService  payment service
     */

    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         CustomerFactory customerFactory,
                         AthleteRepository athleteRepository,
                         CommissionRepository commissionRepository,
                         CartService cartService,
                         PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerFactory = customerFactory;
        this.athleteRepository = athleteRepository;
        this.commissionRepository = commissionRepository;
        this.cartService = cartService;
        this.paymentService = paymentService;
    }

    /**
     * Processes a new order checkout, handling stock validation, athlete discounts, 
     * stock reservation, and PayHere payment initialization
     * <p>
     * Consolidates all requested items into a single order and payment transaction
     * Supports both authenticated and guest checkouts via customer resolution
     *
     * @param request  checkout request details
     * @param loggedInCustomerId ID of the authenticated customer
     * @return order creation response containing the ID, total and PayHere payment URL
     * @throws ResourceNotFoundException    if a requested product or the authenticated customer is not found
     * @throws InsufficientStockException   if any requested item exceeds available stock
     * @throws ProductNotAvailableException if any requested item is unavailable for purchase
     */

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto request, Long loggedInCustomerId) {
        Map<Long, Product> productsById = new HashMap<>();
        List<String> insufficientStockMessages = new ArrayList<>();

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
            productsById.put(itemRequest.productId(), product);

            if (product.getStatus() == ProductStatus.COMING_SOON) {
                throw new ProductNotAvailableException(
                        "\"" + product.getName() + "\" hasn't launched yet and can't be ordered.");
            }

            if (product.getStockQty() == null || product.getStockQty() < itemRequest.quantity()) {
                insufficientStockMessages.add("\"" + product.getName() + "\" (" + itemRequest.colour() + "/" + itemRequest.size() + ")"
                        + " — requested " + itemRequest.quantity()
                        + ", only " + (product.getStockQty() == null ? 0 : product.getStockQty()) + " left");
            }
        }

        if (!insufficientStockMessages.isEmpty()) {
            throw new InsufficientStockException(
                    "Not enough stock for: " + String.join("; ", insufficientStockMessages));
        }

        
        AthleteReferral referral = resolveReferral(request.athleteCode());

        Customer customer = customerFactory.resolveForCheckout(loggedInCustomerId, request);

        BigDecimal subtotal = request.items().stream()
                .map(item -> productsById.get(item.productId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = referral.calculateDiscount(subtotal);

        Order order = new Order(customer, subtotal, discount, referral.athleteCodeForOrder());

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            order.addItem(new OrderItem(order, product, itemRequest.size(), itemRequest.colour(),
                    itemRequest.quantity(), product.getPrice()));
        }

        order = orderRepository.save(order);

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            product.setStockQty(product.getStockQty() - itemRequest.quantity());
            productRepository.save(product);
        }

        referral.recordCommission(commissionRepository, order, discount);

        if (loggedInCustomerId != null) {
            cartService.clearCart(loggedInCustomerId);
        }

        String paymentUrl = paymentService.initiatePayment(order, order.getTotalAmount());

        return new OrderResponseDto(order.getId(), order.getTotalAmount(), paymentUrl);
    }

    /**
     * Marks an order as paid upon PayHere payment confirmation
     * <p>
     * Intended for use by the PayHere webhook. Assumes the webhook signature has 
     * already been verified by the caller. This operation is idempotent to safely 
     * accommodate duplicate webhook dispatches
     *
     * @param orderId order ID
     * @throws ResourceNotFoundException if the order is not found
     */

    @Transactional
    public void confirmPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getPaymentStatus() == OrderStatus.PAID) {
            return; 
        }

        order.setPaymentStatus(OrderStatus.PAID);
        orderRepository.save(order);

        commissionRepository.findByOrderId(order.getId()).ifPresent(commission -> {
            commission.setCommissionStatus(CommissionStatus.EARNED);
            commissionRepository.save(commission);
        });
    }

    /**
     * Retrieves the full details of a specific order for the order confirmation page
     *
     * @param orderId order ID
     * @return order details
     * @throws ResourceNotFoundException if the order is not found
     */

    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetail(Long orderId) {
        return toDetailDto(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId)));
    }

    /**
     * Retrieves a customer's order history, sorted by most recent first
     *
     * @param customerId customer ID
     * @return ordered from newest to oldest
     */

    @Transactional(readOnly = true)
    public List<OrderDetailDto> getOrdersForCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(this::toDetailDto)
                .toList();
    }

    /**
     * Generates the self submitting PayHere checkout HTML for a specific order
     * <p>
     * Executed within a single transaction to ensure lazy associations (such as 
     * {@code order.getItems()} and {@code order.getCustomer()}) are safely 
     * initialized before generating the payment payload, as detailed in the class documentation
     *
     * @param orderId order ID
     * @return complete checkout HTML string
     * @throws ResourceNotFoundException if the order is not found
     */

    @Transactional(readOnly = true)
    public String buildCheckoutRedirectHtml(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return paymentService.buildCheckoutFormHtml(order);
    }

   /**
     * Retrieves all orders for the admin dashboard, optionally filtered by payment status 
     * and sorted by most recent first
     *
     * @param statusFilter payment status to filter 
     * @return matching orders
     */

    @Transactional(readOnly = true)
    public List<AdminOrderDto> getAllOrdersForAdmin(String statusFilter) {
        List<Order> orders;
        if (statusFilter != null && !statusFilter.isBlank()) {
            OrderStatus status = OrderStatus.valueOf(statusFilter.toUpperCase());
            orders = orderRepository.findByPaymentStatus(status);
        } else {
            orders = orderRepository.findAllByOrderByOrderDateDesc();
        }
        return orders.stream().map(this::toAdminDto).toList();
    }

    /**
     * Resolves the athlete referral for a checkout, returning a non null 
     * {@link AthleteReferral} to eliminate explicit null checks
     * <p>
     * Blank, missing or invalid codes safely resolve to the 
     * {@link NoAthleteReferral#INSTANCE} singleton, ensuring the checkout 
     * process proceeds without failure
     *
     * @param athleteCode athlete code submitted at checkout 
     * @return resolved {@link AthleteReferral} implementation
     */

    private AthleteReferral resolveReferral(String athleteCode) {
        if (athleteCode == null || athleteCode.isBlank()) {
            return NoAthleteReferral.INSTANCE;
        }
        return athleteRepository.findByAthleteCodeIgnoreCase(athleteCode)
                .filter(Athlete::isActive)
                .<AthleteReferral>map(AppliedAthleteReferral::new)
                .orElse(NoAthleteReferral.INSTANCE);
    }

    /**
     * Maps a database entity to its public facing DTO representation
     *
     * @param o entity to map
     * @return corresponding DTO
     */

    private OrderDetailDto toDetailDto(Order o) {
        return new OrderDetailDto(
                o.getId(), toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    /**
     * Maps a database entity to its public facing DTO representation for the admin order list
     *
     * @param o entity to map
     * @return corresponding DTO
     */

    private AdminOrderDto toAdminDto(Order o) {
        return new AdminOrderDto(
                o.getId(), o.getCustomer().getName(), o.getCustomer().getPhone(), o.getCustomer().getAddress(),
                toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    /**
     * Maps an order's items to their DTO representations
     * <p>
     * Defined as package private and static to centralize line item mapping logic, 
     * allowing reuse by {@link AthleteService#getDashboard}
     *
     * @param o order containing the items to map
     * @return mapped order item DTOs
     */

    static List<OrderLineItemDto> toLineItemDtos(Order o) {
        return o.getItems().stream()
                .map(i -> new OrderLineItemDto(
                        i.getProduct().getId(), i.getProduct().getName(), i.getSize(), i.getColour(),
                        i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .collect(Collectors.toList());
    }
}
