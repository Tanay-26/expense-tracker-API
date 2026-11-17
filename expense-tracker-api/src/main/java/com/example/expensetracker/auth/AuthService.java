package com.example.expensetracker.auth;

import com.example.expensetracker.auth.dto.AuthResponse;
import com.example.expensetracker.auth.dto.LoginRequest;
import com.example.expensetracker.auth.dto.RegisterRequest;
import com.example.expensetracker.common.DuplicateResourceException;
import com.example.expensetracker.security.AppUserPrincipal;
import com.example.expensetracker.security.JwtService;
import com.example.expensetracker.user.User;
import com.example.expensetracker.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All sign-up / sign-in business rules live here. Controllers stay thin. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        // Application-level check for a friendly 409. The UNIQUE constraint in Postgres is the
        // real guarantee - two concurrent requests could both pass this check, and the DB wins.
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with email " + email + " already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));  // BCrypt hash, one-way
        user.setFullName(request.fullName().trim());
        User saved = userRepository.save(user);

        String token = jwtService.generateToken(AppUserPrincipal.from(saved));
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMinutes(),
                saved.getEmail(), saved.getFullName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Delegating to AuthenticationManager means Spring Security does the BCrypt comparison
        // in constant time and throws BadCredentialsException on failure.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase().trim(), request.password()));

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();

        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMinutes(),
                user.getEmail(), user.getFullName());
    }
}
