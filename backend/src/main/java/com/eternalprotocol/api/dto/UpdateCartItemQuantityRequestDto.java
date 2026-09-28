package com.eternalprotocol.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * The request used to change the quantity of an item already in the cart
 *
 * @param quantity the new quantity 
 */

public record UpdateCartItemQuantityRequestDto(
        @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity
) {}
