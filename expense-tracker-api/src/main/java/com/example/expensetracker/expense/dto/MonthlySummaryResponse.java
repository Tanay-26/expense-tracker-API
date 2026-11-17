package com.example.expensetracker.expense.dto;

import com.example.expensetracker.expense.Category;

import java.math.BigDecimal;
import java.util.List;

/** Response for GET /api/v1/expenses/summary?month=2026-08 */
public record MonthlySummaryResponse(
        String month,
        BigDecimal total,
        List<CategoryBreakdown> breakdown
) {
    public record CategoryBreakdown(Category category, BigDecimal total, long count) {}
}
