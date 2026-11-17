package com.example.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security is the one place where "it seems to work" is not good enough, so the negative cases
 * (tampered signature, expired token, wrong key) matter more than the happy path.
 */
class JwtServiceTest {

    private static final String SECRET =
            "RyX4ZHlQ34AT1+LRh8Z0ygMum6Pwg7pghqVbQ6+XHBG2TNN7OIKubgkL4N5SpZrZ";
    private static final String OTHER_SECRET =
            "Zm9vYmFyLXRoaXMtaXMtYS1jb21wbGV0ZWx5LWRpZmZlcmVudC1zZWNyZXQta2V5MTIz";

    private final JwtService jwtService = new JwtService(SECRET, 60);
    private final AppUserPrincipal principal = new AppUserPrincipal(7L, "riya@example.com", null);

    @Test
    @DisplayName("A freshly issued token carries the email as subject and the id as the uid claim")
    void generateAndParse() {
        String token = jwtService.generateToken(principal);

        Claims claims = jwtService.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("riya@example.com");
        assertThat(claims.get("uid", Number.class).longValue()).isEqualTo(7L);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("A token signed with a different secret is rejected")
    void tokenFromAnotherSecretIsRejected() {
        String foreignToken = new JwtService(OTHER_SECRET, 60).generateToken(principal);

        assertThatThrownBy(() -> jwtService.parseAndValidate(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("An expired token is rejected even though its signature is valid")
    void expiredTokenIsRejected() {
        // Negative expiry = the token expired one minute before it was created.
        String expired = new JwtService(SECRET, -1).generateToken(principal);

        assertThatThrownBy(() -> jwtService.parseAndValidate(expired))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("A tampered payload breaks the signature")
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(principal);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(JwtException.class);
    }
}
