package com.example.expensetracker.expense.dto;

import com.example.expensetracker.expense.Category;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body for POST and PUT /api/v1/expenses. */
public record ExpenseRequest(
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than 0")
        @DecimalMax(value = "9999999999.99") BigDecimal amount,

        @NotNull Category category,

        @Size(max = 255) String description,

        @NotNull @PastOrPresent(message = "spentAt cannot be in the future") LocalDate spentAt
) {}
