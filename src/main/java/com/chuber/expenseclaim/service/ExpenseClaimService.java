package com.chuber.expenseclaim.service;

import com.chuber.expenseclaim.dto.ClaimRequest;
import com.chuber.expenseclaim.dto.DecisionRequest;
import com.chuber.expenseclaim.entity.AppUser;
import com.chuber.expenseclaim.entity.ExpenseClaim;
import com.chuber.expenseclaim.enums.ClaimStatus;
import com.chuber.expenseclaim.enums.Role;
import com.chuber.expenseclaim.repository.ExpenseClaimRepository;
import com.chuber.expenseclaim.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExpenseClaimService {

    private final ExpenseClaimRepository claimRepository;
    private final UserRepository userRepository;

    public ExpenseClaimService(ExpenseClaimRepository claimRepository, UserRepository userRepository) {
        this.claimRepository = claimRepository;
        this.userRepository = userRepository;
    }

    public List<ExpenseClaim> getClaims() {
        AppUser user = getCurrentUser();
        if (user.getRole() == Role.APPROVER) {
            return claimRepository.findAll();
        }
        return claimRepository.findByEmployeeUsername(user.getUsername());
    }

    public ExpenseClaim getClaim(Long id) {
        AppUser user = getCurrentUser();
        ExpenseClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));

        if (user.getRole() == Role.EMPLOYEE && !claim.getEmployee().getUsername().equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot view another employee's claim");
        }

        return claim;
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    public ExpenseClaim submitClaim(ClaimRequest request) {
        AppUser employee = getCurrentUser();

        ExpenseClaim claim = new ExpenseClaim();
        claim.setEmployee(employee);
        claim.setDescription(request.description());
        claim.setAmount(request.amount());
        claim.setExpenseDate(request.expenseDate());
        claim.setCategory(request.category());
        claim.setSubmittedAt(LocalDateTime.now());

        return claimRepository.save(claim);
    }

    @PreAuthorize("hasRole('APPROVER')")
    public ExpenseClaim decide(Long id, DecisionRequest request) {
        AppUser approver = getCurrentUser();

        ExpenseClaim claim = claimRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Claim not found"));

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Claim has already been decided");
        }

        if (request.status() != ClaimStatus.APPROVED && request.status() != ClaimStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }

        if (request.status() == ClaimStatus.REJECTED && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rejection reason is required");
        }

        claim.setStatus(request.status());
        claim.setDecidedBy(approver);
        claim.setDecidedAt(LocalDateTime.now());
        if (request.status() == ClaimStatus.REJECTED) {
            claim.setRejectionReason(request.rejectionReason());
        }

        return claimRepository.save(claim);
    }

    private AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
