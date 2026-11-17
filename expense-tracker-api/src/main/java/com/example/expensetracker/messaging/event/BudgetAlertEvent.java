package com.example.expensetracker.messaging.event;

import com.example.expensetracker.expense.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published to "notification-events" when a user crosses a budget threshold.
 * Severity is decided by the producer (the budget evaluator) so that the notification consumer
 * stays dumb - it only formats and delivers.
 */
public record BudgetAlertEvent(
        String eventId,
        Long userId,
        String userEmail,
        Category category,
        BigDecimal monthlySpend,
        BigDecimal monthlyLimit,
        int percentageUsed,
        String severity,     // WARNING (>=80%) or EXCEEDED (>=100%)
        String period,       // e.g. "2026-08"
        LocalDateTime occurredAt
) {}
