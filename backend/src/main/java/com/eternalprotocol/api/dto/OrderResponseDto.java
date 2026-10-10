package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * Response for a newly created order, containing payment redirection details
 *
 * @param orderId    order ID
 * @param totalAmount total amount
 * @param paymentUrl  payment redirection URL
 */

public record OrderResponseDto(
        Long orderId,
        BigDecimal totalAmount,
        String paymentUrl
) {}
