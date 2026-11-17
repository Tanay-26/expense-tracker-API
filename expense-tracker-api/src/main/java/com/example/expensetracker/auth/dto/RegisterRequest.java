package com.example.expensetracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming JSON for POST /api/v1/auth/register.
 * A record = immutable data carrier with no boilerplate. The annotations are enforced by
 * @Valid in the controller, so bad input never reaches the service layer.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72, message = "password must be 8-72 characters") String password,
        @NotBlank @Size(max = 120) String fullName
) {}
