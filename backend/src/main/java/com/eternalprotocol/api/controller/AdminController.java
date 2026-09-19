package com.eternalprotocol.api.controller;

// MILESTONE 1 SCOPE — four endpoints: list/create/update athletes, list orders.
// Deferred to Milestone 2: the two commission endpoints.

import com.eternalprotocol.api.dto.AdminOrderDto;
import com.eternalprotocol.api.dto.AthleteDto;
import com.eternalprotocol.api.dto.CreateAthleteRequestDto;
import com.eternalprotocol.api.dto.UpdateAthleteRequestDto;
import com.eternalprotocol.api.service.AthleteService;
import com.eternalprotocol.api.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for the admin dashboard: viewing all orders and managing
 * athletes.
 * <p>
 * Every endpoint here requires the ADMIN role. This is enforced centrally
 * in {@code SecurityConfig} for the whole {@code /api/admin/**} path,
 * rather than being checked in each individual method.
 * <p>
 * Product management lives in {@code ProductController} instead, since
 * that controller also serves the public storefront's read endpoints. Order
 * listing lives on Chamod's {@code OrderService} — this class calls it but
 * doesn't own it, since it queries {@code OrderRepository}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final AthleteService athleteService;

    /**
     * @param orderService   order listing logic (Chamod's module)
     * @param athleteService athlete management logic
     */
    public AdminController(OrderService orderService, AthleteService athleteService) {
        this.orderService = orderService;
        this.athleteService = athleteService;
    }

    /**
     * Gets every order in the system, most recent first, optionally
     * filtered by payment status.
     *
     * @param status payment status to filter by (e.g. "PAID"), optional
     * @return matching orders
     */
    @GetMapping("/orders")
    public ResponseEntity<List<AdminOrderDto>> getAllOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin(status));
    }

    /**
     * Gets every athlete account.
     *
     * @return all athletes
     */
    @GetMapping("/athletes")
    public ResponseEntity<List<AthleteDto>> getAllAthletes() {
        return ResponseEntity.ok(athleteService.getAllAthletes());
    }

    /**
     * Creates a new athlete account.
     *
     * @param request new athlete's details
     * @return the created athlete
     */
    @PostMapping("/athletes")
    public ResponseEntity<AthleteDto> createAthlete(@Valid @RequestBody CreateAthleteRequestDto request) {
        return ResponseEntity.ok(athleteService.createAthlete(request));
    }

    /**
     * Updates an existing athlete's commission rate and/or active status.
     *
     * @param id      athlete's database ID
     * @param request fields to change; unset fields are left unchanged
     * @return the updated athlete
     */
    @PutMapping("/athletes/{id}")
    public ResponseEntity<AthleteDto> updateAthlete(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateAthleteRequestDto request) {
        return ResponseEntity.ok(athleteService.updateAthlete(id, request));
    }

    // ------------------------------------------------------------------
    // MILESTONE 2:
    //   GET /api/admin/commissions              getAllCommissions()
    //   PUT /api/admin/commissions/{id}/mark-paid  markCommissionPaid()
    // ------------------------------------------------------------------
}
