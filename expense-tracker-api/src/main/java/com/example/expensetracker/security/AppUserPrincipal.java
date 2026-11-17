package com.example.expensetracker.security;

import com.example.expensetracker.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The object that sits inside SecurityContextHolder for an authenticated request.
 *
 * Why a custom principal instead of Spring's built-in org.springframework.security.core.userdetails.User?
 * Because I need the numeric userId on every request (every query is scoped by user_id). Spring's default
 * principal only carries the username, which would force a SELECT on users for every single API call.
 * I put the id in the JWT and rebuild this object from the token = zero DB hits for authentication.
 */
public class AppUserPrincipal implements UserDetails {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final Long id;
    private final String email;
    private final String password;

    public AppUserPrincipal(Long id, String email, String password) {
        this.id = id;
        this.email = email;
        this.password = password;
    }

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPassword());
    }

    public Long getId() { return id; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return AUTHORITIES; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
