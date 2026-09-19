package com.eternalprotocol.api.controller;

// MILESTONE 1 SCOPE — one public endpoint: validate-code.
// Deferred to Milestone 2: GET /api/athletes/dashboard, and the CurrentUser
// dependency it needs (validate-code is public and needs no logged-in user).

import com.eternalprotocol.api.dto.AthleteCodeValidationDto;
import com.eternalprotocol.api.service.AthleteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for validating discount codes.
 * <p>
 * Athlete account management (create/update by an admin) lives in
 * {@code AdminController} instead, since those actions are performed by an
 * admin, not by the athlete themself.
 */
@RestController
@RequestMapping("/api/athletes")
public class AthleteController {

    private final AthleteService athleteService;

    /**
     * MILESTONE 2 adds {@code CurrentUser} to this constructor, to resolve
     * the logged-in athlete's ID for {@code getDashboard()}.
     *
     * @param athleteService code validation logic
     */
    public AthleteController(AthleteService athleteService) {
        this.athleteService = athleteService;
    }

    /**
     * Public. Checks whether a discount code exists and is active, and
     * returns its discount percentage. Used by the checkout page to show a
     * live discount preview as the customer types a code.
     *
     * @param code the code to check
     * @return whether the code is valid, and its discount percentage
     */
    @GetMapping("/validate-code/{code}")
    public ResponseEntity<AthleteCodeValidationDto> validateCode(@PathVariable String code) {
        return ResponseEntity.ok(athleteService.validateCode(code));
    }

    // ------------------------------------------------------------------
    // MILESTONE 2:
    //   GET /api/athletes/dashboard   getDashboard()   (ATHLETE only)
    // ------------------------------------------------------------------
}
