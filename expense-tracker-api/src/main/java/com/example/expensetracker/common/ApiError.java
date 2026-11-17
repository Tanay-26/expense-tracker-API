package com.example.expensetracker.common;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One consistent error shape for the whole API, so a client never has to guess
 * whether a failure returns {"error":...} or {"message":...}.
 */
public record ApiError(
        int status,
        String error,
        String message,
        List<String> details,
        LocalDateTime timestamp
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, List.of(), LocalDateTime.now());
    }

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(status, error, message, details, LocalDateTime.now());
    }
}
