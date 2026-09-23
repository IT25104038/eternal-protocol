package com.eternalprotocol.api.controller;


import com.eternalprotocol.api.dto.CartItemDto;
import com.eternalprotocol.api.dto.CartItemRequestDto;
import com.eternalprotocol.api.dto.UpdateCartItemQuantityRequestDto;
import com.eternalprotocol.api.security.CurrentUser;
import com.eternalprotocol.api.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for a logged-in customer's own account: the saved cart in
 * Milestone 1, plus profile and order history in Milestone 2.
 * <p>
 * Every endpoint here identifies "which customer" using
 * {@link CurrentUser}, which reads the ID from the verified JWT — never
 * from a path variable — so one customer can never view or edit another
 * customer's profile, orders, or cart just by changing a URL.
 */
@RestController
@RequestMapping("/api/customers/me")
public class CustomerController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    /**
     * MILESTONE 2 adds {@code CustomerService} (profile) and
     * {@code OrderService} (order history) to this constructor, and swaps
     * {@code CartService} for {@code CartInvoker} on the mutating cart
     * endpoints.
     *
     * @param cartService saved cart logic
     * @param currentUser resolves the logged-in customer's ID from the JWT
     */
    public CustomerController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }
}
