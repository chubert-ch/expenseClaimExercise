package com.chuber.expenseclaim.dto;

import com.chuber.expenseclaim.entity.ExpenseClaim;
import com.chuber.expenseclaim.enums.Category;
import com.chuber.expenseclaim.enums.ClaimStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClaimResponse(
        Long id,
        String employee,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        Category category,
        ClaimStatus status,
        LocalDateTime submittedAt,
        String decidedBy,
        LocalDateTime decidedAt,
        String rejectionReason
) {
    public static ClaimResponse from(ExpenseClaim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getEmployee().getUsername(),
                claim.getDescription(),
                claim.getAmount(),
                claim.getExpenseDate(),
                claim.getCategory(),
                claim.getStatus(),
                claim.getSubmittedAt(),
                claim.getDecidedBy() != null ? claim.getDecidedBy().getUsername() : null,
                claim.getDecidedAt(),
                claim.getRejectionReason()
        );
    }
}
