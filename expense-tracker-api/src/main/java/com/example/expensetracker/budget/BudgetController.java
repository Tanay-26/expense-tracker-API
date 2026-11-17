package com.example.expensetracker.budget;

import com.example.expensetracker.budget.dto.BudgetRequest;
import com.example.expensetracker.budget.dto.BudgetResponse;
import com.example.expensetracker.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PutMapping
    public BudgetResponse upsert(@AuthenticationPrincipal AppUserPrincipal principal,
                                 @Valid @RequestBody BudgetRequest request) {
        return budgetService.upsert(principal.getId(), request);
    }

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return budgetService.list(principal.getId());
    }
}
