package com.example.expensetracker.budget.dto;

import com.example.expensetracker.budget.Budget;
import com.example.expensetracker.expense.Category;

import java.math.BigDecimal;

public record BudgetResponse(Long id, Category category, BigDecimal monthlyLimit) {
    public static BudgetResponse from(Budget b) {
        return new BudgetResponse(b.getId(), b.getCategory(), b.getMonthlyLimit());
    }
}
