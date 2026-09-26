package com.eternalprotocol.api.service;

// MILESTONE 1 SCOPE.
// Deferred to Milestone 2:
//   - AthleteCodeCache (Singleton). validateCode() queries the repository
//     directly below — Milestone 2 wraps this exact lookup with a cache
//     check/populate, without changing what it returns.
//   - getDashboard() and the athlete-facing dashboard entirely.
//   - getAllCommissions() and markCommissionPaid(), and CommissionDto.

import com.eternalprotocol.api.dto.AthleteCodeValidationDto;
import com.eternalprotocol.api.dto.AthleteDto;
import com.eternalprotocol.api.dto.CreateAthleteRequestDto;
import com.eternalprotocol.api.dto.UpdateAthleteRequestDto;
import com.eternalprotocol.api.entity.Athlete;
import com.eternalprotocol.api.exception.EmailAlreadyInUseException;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.AthleteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic for discount code validation and admin management of
 * athletes.
 */
@Service
public class AthleteService {

    private final AthleteRepository athleteRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * MILESTONE 2 adds {@code CommissionRepository} to this constructor.
     *
     * @param athleteRepository access to athlete data
     * @param passwordEncoder   hashes new athlete passwords
     */
    public AthleteService(AthleteRepository athleteRepository, PasswordEncoder passwordEncoder) {
        this.athleteRepository = athleteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Checks whether a discount code exists and is active. Used by the
     * checkout page's live discount preview as a customer types a code.
     * <p>
     * MILESTONE 2 wraps this method's body with {@code AthleteCodeCache}
     * (Singleton pattern): check the cache first, and populate it on a
     * miss, since this is a fast-repeating call for the same one or two
     * codes. Keep the return value identical so that wrap is a pure
     * performance refactor.
     *
     * @param code the code to check
     * @return whether the code is valid, and its discount percentage
     */
    public AthleteCodeValidationDto validateCode(String code) {
        return athleteRepository.findByAthleteCodeIgnoreCase(code)
                .filter(Athlete::isActive)
                .map(a -> new AthleteCodeValidationDto(true, a.getAthleteCode(), a.getCommissionRate()))
                .orElseGet(() -> AthleteCodeValidationDto.invalid(code));
    }

    /**
     * Gets every athlete account, for the admin dashboard.
     *
     * @return all athletes
     */
    public List<AthleteDto> getAllAthletes() {
        return athleteRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Creates a new athlete account.
     *
     * @param request new athlete's details
     * @return the created athlete
     * @throws EmailAlreadyInUseException if the email is already registered
     * @throws IllegalArgumentException   if the athlete code is already taken
     */
    public AthleteDto createAthlete(CreateAthleteRequestDto request) {
        if (athleteRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException("An account with this email already exists");
        }
        if (athleteRepository.existsByAthleteCodeIgnoreCase(request.athleteCode())) {
            throw new IllegalArgumentException("Athlete code already in use: " + request.athleteCode());
        }

        Athlete athlete = new Athlete();
        athlete.setName(request.name());
        athlete.setEmail(request.email());
        athlete.setPasswordHash(passwordEncoder.encode(request.password()));
        athlete.setAthleteCode(request.athleteCode());
        athlete.setCommissionRate(request.commissionRate());
        athlete.setActive(true);

        return toDto(athleteRepository.save(athlete));
    }

    /**
     * Updates an existing athlete's commission rate and/or active status.
     * Any field left null in the request is left unchanged.
     * <p>
     * MILESTONE 2 also evicts this athlete's code from
     * {@code AthleteCodeCache} here, since either change affects what
     * {@link #validateCode} should return for it.
     *
     * @param id      athlete's database ID
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

        return toDto(athleteRepository.save(athlete));
    }

    /**
     * Converts a database entity into its public-facing DTO shape.
     *
     * @param a the entity to convert
     * @return the equivalent DTO
     */
    private AthleteDto toDto(Athlete a) {
        return new AthleteDto(a.getId(), a.getName(), a.getEmail(), a.getAthleteCode(),
                a.getCommissionRate(), a.isActive());
    }
}
