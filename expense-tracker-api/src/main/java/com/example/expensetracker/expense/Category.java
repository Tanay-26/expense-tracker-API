package com.example.expensetracker.expense;

/**
 * Fixed set of spend buckets. An enum instead of a categories table because the list is small,
 * stable, and shared by all users - a join table here would add a JOIN to every query for no benefit.
 * Stored as VARCHAR (@Enumerated(STRING)), never as its ordinal, so reordering the enum later
 * cannot silently corrupt existing rows.
 */
public enum Category {
    FOOD,
    TRANSPORT,
    RENT,
    UTILITIES,
    ENTERTAINMENT,
    HEALTH,
    SHOPPING,
    EDUCATION,
    OTHER
}
