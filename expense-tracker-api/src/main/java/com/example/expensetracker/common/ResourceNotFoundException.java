package com.example.expensetracker.common;

/** Thrown by services when an id does not exist (or does not belong to the caller). Mapped to 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
