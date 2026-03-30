package com.chuber.expenseclaim.controller;

import com.chuber.expenseclaim.dto.ClaimRequest;
import com.chuber.expenseclaim.dto.ClaimResponse;
import com.chuber.expenseclaim.dto.DecisionRequest;
import com.chuber.expenseclaim.service.ExpenseClaimService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ExpenseClaimController {

    private final ExpenseClaimService claimService;

    public ExpenseClaimController(ExpenseClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping
    public List<ClaimResponse> getClaims() {
        return claimService.getClaims().stream()
                .map(ClaimResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ClaimResponse getClaim(@PathVariable Long id) {
        return ClaimResponse.from(claimService.getClaim(id));
    }

    @PostMapping
    public ResponseEntity<ClaimResponse> submitClaim(@Valid @RequestBody ClaimRequest request) {
        var claim = claimService.submitClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClaimResponse.from(claim));
    }

    @PutMapping("/{id}/decision")
    public ClaimResponse decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ClaimResponse.from(claimService.decide(id, request));
    }
}
