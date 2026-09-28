package com.eternalprotocol.api.service;


import com.eternalprotocol.api.dto.OrderDetailDto;
import com.eternalprotocol.api.dto.OrderItemRequestDto;
import com.eternalprotocol.api.dto.OrderLineItemDto;
import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.OrderResponseDto;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.dto.AdminOrderDto;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.entity.OrderItem;
import com.eternalprotocol.api.entity.OrderStatus;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.exception.InsufficientStockException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import com.eternalprotocol.api.repository.CustomerRepository;
import com.eternalprotocol.api.repository.OrderRepository;
import com.eternalprotocol.api.repository.ProductRepository;
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
 * Handles the business logic for placing new orders and retrieving order history
 */

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final AthleteRepository athleteRepository;
    private final CommissionRepository commissionRepository;
    private final CartService cartService;

   /**
 * @param orderRepository      access to order data
 * @param productRepository    access to product data for stock checks and pricing
 * @param customerRepository   fetches the logged in customer or creates a guest account
 * @param athleteRepository    looks up athlete discount codes
 * @param commissionRepository saves commissions earned from athlete codes 
 * @param cartService          clears the customer's saved cart after checkout
 */

    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         CustomerRepository customerRepository,
                         AthleteRepository athleteRepository,
                         CommissionRepository commissionRepository,
                         CartService cartService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.athleteRepository = athleteRepository;
        this.commissionRepository = commissionRepository;
        this.cartService = cartService;
    }

    /**
    * Places a new order by validating stock, applying any athlete discounts, 
    * saving the order details, and recording commissions
     * <p>
    * This works for both guests and logged in customers. If {@code loggedInCustomerId} 
    * is null , a new {@code Customer} record is created specifically 
    * for this order
    * <p>
    *
    * @param request            checkout details 
    * @param loggedInCustomerId the loggedin user's ID, or null for guests
    * @return the new order's ID and final total
    * @throws ResourceNotFoundException  if the product or customer cannot be found
    * @throws InsufficientStockException if an item is out of stock
    */

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto request, Long loggedInCustomerId) {

        // Validate stock for all items upfront. If even one item is out of stock, 
        // the entire order should fail cleanly before any changes are saved

        Map<Long, Product> productsById = new HashMap<>();
        List<String> insufficientStockMessages = new ArrayList<>();

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemRequest.productId()));
            productsById.put(itemRequest.productId(), product);

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

        // Reuse the logged in account if present, otherwise create a
        // fresh guest row
        
        Customer customer;
        if (loggedInCustomerId != null) {
            customer = customerRepository.findById(loggedInCustomerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + loggedInCustomerId));
        } else {
            customer = new Customer(request.customerName(), null, null, request.phone(), request.address());
            customer = customerRepository.save(customer);
        }

        BigDecimal subtotal = request.items().stream()
                .map(item -> productsById.get(item.productId()).getPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Athlete code, if any, applies once to the whole order
        // replaces this block with AthleteReferral,
        // so nothing here needs an "if there was an athlete" check

        String athleteCode = null;
        BigDecimal discount = BigDecimal.ZERO;
        Athlete athlete = null;
        if (request.athleteCode() != null && !request.athleteCode().isBlank()) {
            athlete = athleteRepository.findByAthleteCodeIgnoreCase(request.athleteCode())
                    .filter(Athlete::isActive)
                    .orElse(null);
            if (athlete != null) {
                athleteCode = athlete.getAthleteCode();
                discount = subtotal.multiply(athlete.getCommissionRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }

        Order order = new Order(customer, subtotal, discount, athleteCode);

        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            order.addItem(new OrderItem(order, product, itemRequest.size(), itemRequest.colour(),
                    itemRequest.quantity(), product.getPrice()));
        }

        order = orderRepository.save(order);

        // Reserve stock at order creation time , not
        // on payment confirmation — a known read then-write race, kept
        // unchanged from the original
        
        for (OrderItemRequestDto itemRequest : request.items()) {
            Product product = productsById.get(itemRequest.productId());
            product.setStockQty(product.getStockQty() - itemRequest.quantity());
            productRepository.save(product);
        }

        // A real athlete referral always earns a commission

        if (athlete != null) {
            Commission commission = new Commission(athlete, order, discount);
            
            commission.setCommissionStatus(com.eternalprotocol.api.entity.CommissionStatus.EARNED);
            commissionRepository.save(commission);
        }

        // Clear the persisted cart so items just bought don't still show
        
        if (loggedInCustomerId != null) {
            cartService.clearCart(loggedInCustomerId);
        }

        // calls PaymentService.initiatePayment() here instead
        // and returns its redirect URL. Milestone 1 has no payment gateway,
        // so the order is marked paid immediately

        order.setPaymentStatus(OrderStatus.PAID);
        order = orderRepository.save(order);

        return new OrderResponseDto(order.getId(), order.getTotalAmount());
    }

    /**
     * Gets full details of a single order, used for the order confirmation
     * page
     *
     * @param orderId order's database ID
     * @return the order's full details
     * @throws ResourceNotFoundException if no order exists with this ID
     */
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetail(Long orderId) {
        return toDetailDto(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId)));
    }

    /**
     * Gets a customer's order history, most recent first
     * <p>
     * calls this from {@code CustomerController.getOrderHistory}
     *
     * @param customerId customer's database ID
     * @return that customer's orders, newest first
     */
    @Transactional(readOnly = true)
    public List<OrderDetailDto> getOrdersForCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(this::toDetailDto)
                .toList();
    }

    /**
     *  {@code AdminController}
     *
     * @param statusFilter payment status name to filter 
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
     * Converts a database entity into its public facing DTO shape for the
     * admin order list.
     *
     * @param o the entity to convert
     * @return the equivalent DTO
     */
    private AdminOrderDto toAdminDto(Order o) {
        return new AdminOrderDto(
                o.getId(), o.getCustomer().getName(), o.getCustomer().getPhone(), o.getCustomer().getAddress(),
                toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    /**
     * Converts a database entity into its public facing DTO shape
     *
     * @param o the entity to convert
     * @return the equivalent DTO
     */
    private OrderDetailDto toDetailDto(Order o) {
        return new OrderDetailDto(
                o.getId(), toLineItemDtos(o), o.getSubtotalAmount(), o.getDiscountAmount(), o.getTotalAmount(),
                o.getPaymentStatus().name(), o.getAthleteCode(), o.getOrderDate());
    }

    /**
     * Converts an order's items into their DTO shape. Package private and
     * {@code AdminController} 
     * {@code AthleteService}  
     * line item mapping instead of duplicating it
     *
     * @param o the order whose items should be converted
     * @return the order's items as DTOs
     */
    static List<OrderLineItemDto> toLineItemDtos(Order o) {
        return o.getItems().stream()
                .map(i -> new OrderLineItemDto(
                        i.getProduct().getId(), i.getProduct().getName(), i.getSize(), i.getColour(),
                        i.getQuantity(), i.getUnitPrice(), i.getLineTotal()))
                .collect(Collectors.toList());
    }
}
