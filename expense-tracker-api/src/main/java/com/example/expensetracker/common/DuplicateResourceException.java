package com.example.expensetracker.common;

/** Thrown when a uniqueness rule is violated, e.g. registering an email twice. Mapped to 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
