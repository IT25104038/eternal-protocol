package com.eternalprotocol.api.dto;

import java.math.BigDecimal;

/**
 * A purchased item from a completed order
 * 
 * We reuse this exact structure in {@link OrderDetailDto}, {@link AdminOrderDto}, 
 * and {@link AthleteOrderSummaryDto} to avoid repeating code
 *
 * @param productId   purchased product's ID
 * @param productName purchased product's name
 * @param size        size purchased
 * @param colour      colour purchased
 * @param quantity    quantity purchased
 * @param unitPrice   price per unit at the time of purchase
 * @param lineTotal   total price for this item 
 */

public record OrderLineItemDto(
        Long productId,
        String productName,
        String size,
        String colour,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
