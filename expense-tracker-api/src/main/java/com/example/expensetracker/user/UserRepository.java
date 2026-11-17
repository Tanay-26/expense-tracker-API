package com.example.expensetracker.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data generates the implementation at startup from the method names.
 * findByEmail -> "select u from User u where u.email = ?1" (uses the UNIQUE index on email).
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
