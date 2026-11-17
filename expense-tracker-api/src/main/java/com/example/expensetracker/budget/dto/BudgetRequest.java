package com.example.expensetracker.budget.dto;

import com.example.expensetracker.expense.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Body for PUT /api/v1/budgets - an upsert, hence PUT (idempotent) rather than POST. */
public record BudgetRequest(
        @NotNull Category category,
        @NotNull @DecimalMin(value = "1.00", message = "monthlyLimit must be at least 1") BigDecimal monthlyLimit
) {}
