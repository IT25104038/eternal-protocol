package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;

import java.math.BigDecimal;

/**
 * Represents the athlete referral state for a checkout
 * <p>
 * Implements the Null Object pattern to avoid nullable {@code Athlete} checks
 * Checkouts hold either an {@link AppliedAthleteReferral} or the 
 * {@link NoAthleteReferral} singleton, allowing callers to calculate discounts 
 * and record commissions without explicit null handling
 */

public interface AthleteReferral {

    /**
     * Calculates the discount applied to the order subtotal by this referral
     *
     * @param subtotal the order subtotal before discounts
     * @return the discount amount  {@link BigDecimal#ZERO} if no referral applies
     */

    BigDecimal calculateDiscount(BigDecimal subtotal);

   /**
     * Retrieves the athlete code to be recorded on the order
     *
     * @return the athlete code, or {@code null} if no referral applies
     */

    String athleteCodeForOrder();

    /**
     * Records a commission for the referral against a saved order
     * <p>
     * Implemented as a no op by {@link NoAthleteReferral} to allow unconditional 
     * execution by the {@code OrderService} without explicit null checks
     *
     * @param commissionRepository commission repository
     * @param order    saved order
     * @param discountAmount  applied discount
     */

    void recordCommission(CommissionRepository commissionRepository, Order order, BigDecimal discountAmount);
}
