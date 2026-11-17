package com.example.expensetracker.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * Ownership check and fetch in ONE query. Doing findById() then comparing the owner in Java
     * is both slower and leaks information (a 403 vs 404 tells an attacker the id exists).
     */
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    /** Uses idx_expenses_user_spent_at (user_id, spent_at DESC) - index range scan, no sort step. */
    Page<Expense> findByUserIdAndSpentAtBetween(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    /** Uses idx_expenses_user_cat_spent_at. */
    Page<Expense> findByUserIdAndCategoryAndSpentAtBetween(Long userId, Category category,
                                                           LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Monthly total for one category. COALESCE avoids a null when the user has no expenses yet.
     * Aggregation is done by Postgres, not by pulling rows into the JVM and summing them.
     */
    @Query("""
           SELECT COALESCE(SUM(e.amount), 0)
           FROM Expense e
           WHERE e.user.id = :userId
             AND e.category = :category
             AND e.spentAt BETWEEN :from AND :to
           """)
    BigDecimal sumByUserCategoryAndPeriod(@Param("userId") Long userId,
                                          @Param("category") Category category,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    /** One GROUP BY round-trip powers the whole /summary endpoint. */
    @Query("""
           SELECT e.category AS category, SUM(e.amount) AS total, COUNT(e) AS count
           FROM Expense e
           WHERE e.user.id = :userId
             AND e.spentAt BETWEEN :from AND :to
           GROUP BY e.category
           ORDER BY SUM(e.amount) DESC
           """)
    List<CategoryTotal> summarizeByCategory(@Param("userId") Long userId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);
}
