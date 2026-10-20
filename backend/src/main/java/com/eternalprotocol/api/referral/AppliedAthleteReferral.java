package com.eternalprotocol.api.referral;

import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.CommissionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents a valid athlete referral applied during checkout
 * <p>
 * See {@link AthleteReferral} for the rationale behind using this alongside 
 * {@link NoAthleteReferral} instead of a nullable {@code Athlete}
 */

public final class AppliedAthleteReferral implements AthleteReferral {

    private final Athlete athlete;

    /**
     * @param athlete the verified athlete associated with the referral
     */

    public AppliedAthleteReferral(Athlete athlete) {
        this.athlete = athlete;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Computed from the athlete's {@code commissionRate}, which serves as both the customer discount and athlete commission per {@link Athlete#getCommissionRate()}.
     */

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return subtotal.multiply(athlete.getCommissionRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String athleteCodeForOrder() {
        return athlete.getAthleteCode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always records a new {@link Commission} for the referred athlete.
     */
    
    @Override
    public void recordCommission(CommissionRepository commissionRepository, Order order, BigDecimal discountAmount) {
        commissionRepository.save(new Commission(athlete, order, discountAmount));
    }
}
