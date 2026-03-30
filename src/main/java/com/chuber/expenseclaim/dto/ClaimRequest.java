package com.chuber.expenseclaim.dto;

import com.chuber.expenseclaim.enums.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal amount,
        @NotNull LocalDate expenseDate,
        @NotNull Category category
) {
}
