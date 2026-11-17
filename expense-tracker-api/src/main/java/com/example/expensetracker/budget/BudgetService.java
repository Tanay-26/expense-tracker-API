package com.example.expensetracker.budget;

import com.example.expensetracker.budget.dto.BudgetRequest;
import com.example.expensetracker.budget.dto.BudgetResponse;
import com.example.expensetracker.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    /** Upsert: create the budget if it does not exist, otherwise just change the limit. */
    @Transactional
    public BudgetResponse upsert(Long userId, BudgetRequest request) {
        Budget budget = budgetRepository.findByUserIdAndCategory(userId, request.category())
                .orElseGet(() -> {
                    Budget b = new Budget();
                    b.setUser(userRepository.getReferenceById(userId));
                    b.setCategory(request.category());
                    return b;
                });
        budget.setMonthlyLimit(request.monthlyLimit());
        return BudgetResponse.from(budgetRepository.save(budget));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId) {
        return budgetRepository.findByUserIdOrderByCategoryAsc(userId).stream()
                .map(BudgetResponse::from)
                .toList();
    }
}
