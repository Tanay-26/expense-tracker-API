package com.example.expensetracker.expense.dto;

import com.example.expensetracker.expense.Category;
import com.example.expensetracker.expense.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Outgoing JSON. Notice there is no user field - a client only ever sees its own data anyway. */
public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        Category category,
        String description,
        LocalDate spentAt,
        LocalDateTime createdAt
) {
    public static ExpenseResponse from(Expense e) {
        return new ExpenseResponse(e.getId(), e.getAmount(), e.getCategory(),
                e.getDescription(), e.getSpentAt(), e.getCreatedAt());
    }
}
