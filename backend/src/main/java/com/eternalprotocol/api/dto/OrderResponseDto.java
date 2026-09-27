package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * The response sent back immediately after a successful checkout
 *
 * @param orderId     newly created order's ID
 * @param totalAmount final amount paid
 */

public record OrderResponseDto(
        Long orderId,
        BigDecimal totalAmount
) {}
