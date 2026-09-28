package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.OrderDetailDto;
import com.eternalprotocol.api.dto.OrderRequestDto;
import com.eternalprotocol.api.dto.OrderResponseDto;
import com.eternalprotocol.api.security.AppUserDetails;
import com.eternalprotocol.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Handles the checkout process and fetching order details.
 * 
 * These endpoints are public so guest users can easily check out without an account. 
 * However, if a user is logged in, we detect it and link the order to their account history.
 */

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    /**
    * Sets up the order controller
    *
    * @param orderService handles creating orders and calculating totals
    */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
    * Processes a checkout. 
    * Works for both guests and logged in users. If they are logged in, the order gets 
    * saved to their account history. If they are a guest, we create a temporary record 
    * for this specific order.
    *
    * @param request        checkout details: items, delivery info, optional athlete code
     * @param authentication the current request's login info, if any
    * @return the created order's ID and total
    */

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request,
                                                          Authentication authentication) {
        Long loggedInCustomerId = null;
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails user
                && "CUSTOMER".equals(user.getRole())) {
            loggedInCustomerId = user.getId();
        }
        return ResponseEntity.ok(orderService.createOrder(request, loggedInCustomerId));
    }

    /**
     * Gets full details of one order, used for the order confirmation page.
     *
     * @param id order's database ID
     * @return the order's full details
     */
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderDetail(id));
    }

}
