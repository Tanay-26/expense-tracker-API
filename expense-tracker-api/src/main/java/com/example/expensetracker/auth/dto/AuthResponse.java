package com.example.expensetracker.auth.dto;

/**
 * What the client receives after register/login. Note it never contains the password hash -
 * DTOs exist precisely so entity fields cannot leak out of the API by accident.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        String email,
        String fullName
) {}
