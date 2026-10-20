package com.eternalprotocol.api.service;

import com.eternalprotocol.api.cache.AthleteCodeCache;
import com.eternalprotocol.api.dto.AthleteCodeValidationDto;
import com.eternalprotocol.api.dto.AthleteDashboardDto;
import com.eternalprotocol.api.dto.CommissionDto;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.CommissionStatus;
import com.eternalprotocol.api.entity.Customer;
import com.eternalprotocol.api.entity.Order;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for athlete discount codes, the athlete dashboard, and the admin
 * paying out commissions.
 */
@ExtendWith(MockitoExtension.class)
class AthleteDashboardCommissionsAndAdminTest {

    @Mock
    private AthleteRepository athleteRepository;
    @Mock
    private CommissionRepository commissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AthleteService athleteService;

    private Athlete sara;

    @BeforeEach
    void setUp() {
        // The discount-code cache is shared by the whole app, so start every test with it empty
        AthleteCodeCache.getInstance().clear();

        sara = new Athlete("Sara", "sara@test.com", "HASHED", "SARA10", new BigDecimal("10.00"), true);
        sara.setId(1L);
    }

    /** Builds a commission of `amount` in the given status, for an order worth 200.00. */
    private Commission commission(long orderId, String amount, CommissionStatus status) {
        Order order = new Order(new Customer("Nimal", "0771234567", "Colombo"),
                new BigDecimal("200.00"), new BigDecimal(amount), "SARA10");
        order.setId(orderId);

        Commission commission = new Commission(sara, order, new BigDecimal(amount));
        commission.setCommissionStatus(status);
        return commission;
    }

    @Test
    void validateCode_forActiveAthlete_isValidWithTheirDiscount() {
        // Given: an active athlete with the code SARA10
        when(athleteRepository.findByAthleteCodeIgnoreCase("SARA10")).thenReturn(Optional.of(sara));

        // When
        AthleteCodeValidationDto result = athleteService.validateCode("SARA10");

        // Then
        assertTrue(result.valid());
        assertEquals(new BigDecimal("10.00"), result.discountPercentage());
    }

    @Test
    void validateCode_forDeactivatedAthlete_isNotValid() {
        // Given: the athlete has been deactivated by an admin
        sara.setActive(false);
        when(athleteRepository.findByAthleteCodeIgnoreCase("SARA10")).thenReturn(Optional.of(sara));

        // When
        AthleteCodeValidationDto result = athleteService.validateCode("SARA10");

        // Then: the code no longer gives a discount
        assertFalse(result.valid());
    }

    @Test
    void getDashboard_addsUpCommissionByStatus() {
        // Given: three referred orders, each worth 200.00
        when(athleteRepository.findById(1L)).thenReturn(Optional.of(sara));
        when(commissionRepository.findByAthleteId(1L)).thenReturn(List.of(
                commission(1L, "30.00", CommissionStatus.PAID),
                commission(2L, "20.00", CommissionStatus.EARNED),
                commission(3L, "10.00", CommissionStatus.AWAITING_PAYMENT)));

        // When
        AthleteDashboardDto dashboard = athleteService.getDashboard(1L);

        // Then
        assertEquals("SARA10", dashboard.athleteCode());
        assertEquals(3, dashboard.totalOrders());
        assertEquals(new BigDecimal("600.00"), dashboard.salesGenerated());   // 3 orders x 200.00
        assertEquals(new BigDecimal("50.00"), dashboard.commissionEarned());  // PAID 30 + EARNED 20
        assertEquals(new BigDecimal("30.00"), dashboard.paidCommission());    // PAID only
        assertEquals(new BigDecimal("20.00"), dashboard.pendingCommission()); // EARNED, not yet paid out
        // The 10.00 still awaiting payment is not counted anywhere yet
    }

    @Test
    void markCommissionPaid_forEarnedCommission_becomesPaidWithADate() {
        // Given: a commission the athlete has earned but not been paid for
        Commission earned = commission(1L, "20.00", CommissionStatus.EARNED);
        when(commissionRepository.findById(1L)).thenReturn(Optional.of(earned));
        when(commissionRepository.save(any(Commission.class))).thenAnswer(call -> call.getArgument(0));

        // When: the admin pays it out
        CommissionDto result = athleteService.markCommissionPaid(1L);

        // Then
        assertEquals("PAID", result.commissionStatus());
        assertNotNull(result.paidDate());
    }

    @Test
    void markCommissionPaid_whenCustomerHasNotPaidYet_isRefused() {
        // Given: the order behind this commission has not been paid, so nothing is earned yet
        Commission awaiting = commission(1L, "20.00", CommissionStatus.AWAITING_PAYMENT);
        when(commissionRepository.findById(1L)).thenReturn(Optional.of(awaiting));

        // When / Then: the admin cannot pay out money that has not been earned
        assertThrows(IllegalArgumentException.class, () -> athleteService.markCommissionPaid(1L));
        verify(commissionRepository, never()).save(any(Commission.class));
    }
}
