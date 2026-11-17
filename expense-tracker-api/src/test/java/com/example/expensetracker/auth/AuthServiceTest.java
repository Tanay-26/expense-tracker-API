package com.example.expensetracker.auth;

import com.example.expensetracker.auth.dto.AuthResponse;
import com.example.expensetracker.auth.dto.RegisterRequest;
import com.example.expensetracker.common.DuplicateResourceException;
import com.example.expensetracker.security.JwtService;
import com.example.expensetracker.user.User;
import com.example.expensetracker.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService =
            new JwtService("RyX4ZHlQ34AT1+LRh8Z0ygMum6Pwg7pghqVbQ6+XHBG2TNN7OIKubgkL4N5SpZrZ", 60);

    private AuthService service() {
        return new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService);
    }

    @Test
    @DisplayName("register() stores a BCrypt hash, never the raw password")
    void passwordIsHashed() {
        when(userRepository.existsByEmail("riya@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse response = service().register(
                new RegisterRequest("Riya@Example.com", "sup3rsecret", "Riya Sharma"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("riya@example.com");        // normalised to lowercase
        assertThat(saved.getPassword()).isNotEqualTo("sup3rsecret");
        assertThat(saved.getPassword()).startsWith("$2a$");                 // BCrypt marker
        assertThat(response.token()).isNotBlank();
    }

    @Test
    @DisplayName("register() with an existing email returns 409, not a database error")
    void duplicateEmailRejected() {
        when(userRepository.existsByEmail("riya@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service().register(
                new RegisterRequest("riya@example.com", "sup3rsecret", "Riya Sharma")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }
}
