package com.eternalprotocol.api.service;

import com.eternalprotocol.api.cache.AthleteCodeCache;
import com.eternalprotocol.api.dto.*;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.entity.Commission;
import com.eternalprotocol.api.entity.CommissionStatus;
import com.eternalprotocol.api.exception.EmailAlreadyInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.AthleteRepository;
import com.eternalprotocol.api.repository.CommissionRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for discount code validation, an athlete's own dashboard,
 * and admin management of athletes and their commissions
 */

@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final CommissionRepository commissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AthleteCodeGenerator athleteCodeGenerator;

    /**
     * @param athleteRepository  access to athlete data
     * @param commissionRepository access to commission data
     * @param passwordEncoder    hashes new athlete passwords
     * @param athleteCodeGenerator creates a code from the athlete's name when none is supplied
     */
    public AthleteService(AthleteRepository athleteRepository,
                           CommissionRepository commissionRepository,
                           PasswordEncoder passwordEncoder,
                           AthleteCodeGenerator athleteCodeGenerator) {
        this.athleteRepository = athleteRepository;
        this.commissionRepository = commissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.athleteCodeGenerator = athleteCodeGenerator;
    }

    /**
     *
     * @param code the code to check
     * @return whether the code is valid and its discount percentage
     */

    public AthleteCodeValidationDto validateCode(String code) {
        AthleteCodeCache cache = AthleteCodeCache.getInstance();

        return cache.get(code).orElseGet(() -> {
            AthleteCodeValidationDto result = athleteRepository.findByAthleteCodeIgnoreCase(code)
                    .filter(Athlete::isActive)
                    .map(a -> new AthleteCodeValidationDto(true, a.getAthleteCode(), a.getCommissionRate()))
                    .orElseGet(() -> AthleteCodeValidationDto.invalid(code));
            cache.put(code, result);
            return result;
        });
    }

    /**
     *
     * @param athleteId athlete's database ID taken from the JWT
     * @return the athlete's dashboard totals and order list
     * @throws ResourceNotFoundException if no athlete exists with this ID
     */
    @Transactional(readOnly = true)
    public AthleteDashboardDto getDashboard(Long athleteId) {
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found"));

        List<Commission> commissions = commissionRepository.findByAthleteId(athleteId);

        BigDecimal salesGenerated = commissions.stream()
                .map(c -> c.getOrder().getSubtotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal earned = sumByStatus(commissions, CommissionStatus.EARNED)
                .add(sumByStatus(commissions, CommissionStatus.PAID));
        BigDecimal paid = sumByStatus(commissions, CommissionStatus.PAID);
        BigDecimal pending = sumByStatus(commissions, CommissionStatus.EARNED);

        List<AthleteOrderSummaryDto> orderSummaries = commissions.stream()
                .map(c -> new AthleteOrderSummaryDto(
                        c.getOrder().getId(),
                        OrderService.toLineItemDtos(c.getOrder()),
                        c.getOrder().getOrderDate(),
                        c.getOrder().getPaymentStatus().name(),
                        c.getCommissionAmount(),
                        c.getCommissionStatus().name()))
                .toList();

        return new AthleteDashboardDto(
                athlete.getAthleteCode(), athlete.getCommissionRate(),
                commissions.size(), salesGenerated, earned, paid, pending, orderSummaries);
    }

    /**
     * Sums the commission amount of every commission in a given status
     *
     * @param commissions the commissions to sum
     * @param status   status to filter by before summing
     * @return the total amount for commissions in that status
     */
    private BigDecimal sumByStatus(List<Commission> commissions, CommissionStatus status) {
        return commissions.stream()
                .filter(c -> c.getCommissionStatus() == status)
                .map(Commission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gets every athlete account for the admin dashboard
     *
     * @return all athletes
     */
    public List<AthleteDto> getAllAthletes() {
        return athleteRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Creates a new athlete account. If the request has no athlete code, one
     * is generated from the athlete's name (see {@link AthleteCodeGenerator})
     * An admin can still type their own code which must not already be taken
     *
     * @param request new athlete's details
     * @return the created athlete including the final athlete code
     * @throws EmailAlreadyInUseException if the email is already registered
     * @throws IllegalArgumentException   if a manually supplied athlete code is already taken
     */
    public AthleteDto createAthlete(CreateAthleteRequestDto request) {
        if (athleteRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException("An account with this email already exists");
        }

        String athleteCode;
        if (request.athleteCode() == null || request.athleteCode().isBlank()) {
            athleteCode = athleteCodeGenerator.generate(request.name());
        } else {
            athleteCode = request.athleteCode().trim();
            if (athleteRepository.existsByAthleteCodeIgnoreCase(athleteCode)) {
                throw new IllegalArgumentException("Athlete code already in use: " + athleteCode);
            }
        }

        Athlete athlete = new Athlete();
        athlete.setName(request.name());
        athlete.setEmail(request.email());
        athlete.setPasswordHash(passwordEncoder.encode(request.password()));
        athlete.setAthleteCode(athleteCode);
        athlete.setCommissionRate(request.commissionRate());
        athlete.setActive(true);

        AthleteDto created = toDto(athleteRepository.save(athlete));
        AthleteCodeCache.getInstance().evict(athleteCode);
        return created;
    }

    /**
     *
     * @param id    athlete's database ID
     * @param request fields to change
     * @return the updated athlete
     * @throws ResourceNotFoundException if no athlete exists with this ID
     */
    public AthleteDto updateAthlete(Long id, UpdateAthleteRequestDto request) {
        Athlete athlete = athleteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Athlete not found: " + id));

        if (request.commissionRate() != null) {
            athlete.setCommissionRate(request.commissionRate());
        }
        if (request.active() != null) {
            athlete.setActive(request.active());
        }

        AthleteDto updated = toDto(athleteRepository.save(athlete));
        AthleteCodeCache.getInstance().evict(athlete.getAthleteCode());
        return updated;
    }

    /**
     * Gets every commission in the system
     *
     * @param statusFilter status name to filter 
     * @return matching commissions
     */
    @Transactional(readOnly = true)
    public List<CommissionDto> getAllCommissions(String statusFilter) {
        List<Commission> commissions;
        if (statusFilter != null && !statusFilter.isBlank()) {
            CommissionStatus status = CommissionStatus.valueOf(statusFilter.toUpperCase());
            commissions = commissionRepository.findByCommissionStatus(status);
        } else {
            commissions = commissionRepository.findAll();
        }
        return commissions.stream().map(this::toCommissionDto).toList();
    }

    /**
     * Marks a commission as paid out to its athlete
     *
     * @param commissionId commission's database ID
     * @return the updated commission
     * @throws ResourceNotFoundException if no commission exists with this ID
     * @throws IllegalArgumentException  if the commission isn't currently in EARNED status
     */
    @Transactional
    public CommissionDto markCommissionPaid(Long commissionId) {
        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Commission not found: " + commissionId));

        if (commission.getCommissionStatus() != CommissionStatus.EARNED) {
            throw new IllegalArgumentException(
                    "Only commissions with status EARNED can be marked as paid (current: "
                            + commission.getCommissionStatus() + ")");
        }

        commission.setCommissionStatus(CommissionStatus.PAID);
        commission.setPaidDate(LocalDateTime.now());
        return toCommissionDto(commissionRepository.save(commission));
    }

    /**
     * Converts a database entity into its public facing DTO shape
     *
     * @param a entity to convert
     * @return equivalent DTO
     */
    private AthleteDto toDto(Athlete a) {
        return new AthleteDto(a.getId(), a.getName(), a.getEmail(), a.getAthleteCode(),
                a.getCommissionRate(), a.isActive());
    }

    /**
     * Converts a database entity into its public facing DTO shape
     *
     * @param c entity to convert
     * @return equivalent DTO
     */
    private CommissionDto toCommissionDto(Commission c) {
        return new CommissionDto(c.getId(), c.getAthlete().getName(), c.getAthlete().getAthleteCode(),
                c.getOrder().getId(), c.getCommissionAmount(), c.getCommissionStatus().name(), c.getPaidDate());
    }
}
