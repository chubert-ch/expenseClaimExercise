package com.chuber.expenseclaim.dto;

import com.chuber.expenseclaim.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
        @NotNull ClaimStatus status,
        String rejectionReason
) {
}
