package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;

import java.math.BigDecimal;

/**
 * Represents the absence of a valid athlete referral
 * <p>
 * Implements the Null Object pattern for {@link AthleteReferral}, ensuring all 
 * operations safely default to no effect. Accessed as a stateless singleton via {@link #INSTANCE}
 */

public final class NoAthleteReferral implements AthleteReferral {

    public static final NoAthleteReferral INSTANCE = new NoAthleteReferral();

    private NoAthleteReferral() {
    }

    /** @return {@link BigDecimal#ZERO} indicating no discount is applied */
    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    /** @return {@code null}, indicating no athlete code is recorded */
    @Override
    public String athleteCodeForOrder() {
        return null;
    }

   /**
     * No op implementation, ensuring no commission is recorded when an athlete referral is absent
     */

    @Override
    public void recordCommission(CommissionRepository commissionRepository, Order order, BigDecimal discountAmount) {
    }
}
