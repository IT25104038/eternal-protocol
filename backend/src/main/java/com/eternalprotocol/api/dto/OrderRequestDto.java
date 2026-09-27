package com.eternalprotocol.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * The request body used for placing a new order
 * 
 * It contains the delivery details, an optional discount code, and the 
 * list of {@code items} being purchased (represented by {@link OrderItemRequestDto})
 *
 * @param customerName name for delivery
 * @param phone        contact number for delivery
 * @param address      delivery address
 * @param items        products being purchased 
 * @param athleteCode  optional discount code
 */

public record OrderRequestDto(
        @NotBlank(message = "Customer name is required") String customerName,
        @NotBlank(message = "Phone number is required") String phone,
        @NotBlank(message = "Delivery address is required") String address,
        @NotEmpty(message = "At least one item is required") @Valid List<OrderItemRequestDto> items,
        String athleteCode
) {}
