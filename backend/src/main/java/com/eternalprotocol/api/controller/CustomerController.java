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
 * Handles everything related to a logged in customer's personal account, 
 * starting with their shopping cart
 * 
 * use the JWT token to figure out who is logged in
 * This keeps things secure, meaning someone can't snoop on another person's cart 
 * just by changing the ID number in the URL
 */

@RestController
@RequestMapping("/api/customers/me")
public class CustomerController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    /**
     * Sets up the controller with the necessary services
     *
     * @param cartService handles the shopping cart logic
     * @param currentUser extracts the logged-in customer's ID from their token
     */
    public CustomerController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    /**
     * Gets the logged in customer's saved cart
     *
     * @return the customer's current cart lines
     */
    @GetMapping("/cart")
    public ResponseEntity<List<CartItemDto>> getCart() {
        return ResponseEntity.ok(cartService.getCart(currentUser.id()));
    }

    /**
    * Adds an item to the cart. If the exact same item is already in there, 
    * it just bumps up the quantity instead of making a duplicate entry.
    *
    * @param request item to add
    * @return the added or updated cart line
    */

    
    @PostMapping("/cart")
    public ResponseEntity<CartItemDto> addToCart(@Valid @RequestBody CartItemRequestDto request) {
        return ResponseEntity.ok(cartService.addToCart(currentUser.id(), request));
    }

    /**
    * Updates how many of a specific item are in the cart
    *
    * @param itemId  cart line's ID
    * @param request new quantity
     * @return the updated cart line
    */

    @PutMapping("/cart/{itemId}")
    public ResponseEntity<CartItemDto> updateCartItemQuantity(
            @PathVariable Long itemId, @Valid @RequestBody UpdateCartItemQuantityRequestDto request) {
        return ResponseEntity.ok(cartService.updateQuantity(currentUser.id(), itemId, request.quantity()));
    }

    /**
    * Removes an item completely from the cart
    *
    * @param itemId cart line's ID
    * @return empty 204 response
    */

    @DeleteMapping("/cart/{itemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long itemId) {
        cartService.removeFromCart(currentUser.id(), itemId);
        return ResponseEntity.noContent().build();
    }

}
