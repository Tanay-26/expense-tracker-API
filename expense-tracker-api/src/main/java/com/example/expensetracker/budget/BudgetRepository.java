package com.example.expensetracker.budget;

import com.example.expensetracker.expense.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /** Hit by the Kafka consumer on every expense event - served by the unique index. */
    Optional<Budget> findByUserIdAndCategory(Long userId, Category category);

    List<Budget> findByUserIdOrderByCategoryAsc(Long userId);
}
