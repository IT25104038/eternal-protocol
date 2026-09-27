package com.eternalprotocol.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param id              the order's database ID
 * @param items           the products purchased
 * @param subtotalAmount  total cost before discounts
 * @param discountAmount  amount saved from discounts
 * @param totalAmount     final amount paid
 * @param paymentStatus   current payment status
 * @param athleteCode     the applied athlete code 
 * @param orderDate       when the order was placed
 */

public record OrderDetailDto(
        Long id,
        List<OrderLineItemDto> items,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String paymentStatus,
        String athleteCode,
        LocalDateTime orderDate
) {}
