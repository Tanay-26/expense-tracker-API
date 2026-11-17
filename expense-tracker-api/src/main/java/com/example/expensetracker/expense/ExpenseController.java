package com.example.expensetracker.expense;

import com.example.expensetracker.common.PageResponse;
import com.example.expensetracker.expense.dto.ExpenseRequest;
import com.example.expensetracker.expense.dto.ExpenseResponse;
import com.example.expensetracker.expense.dto.MonthlySummaryResponse;
import com.example.expensetracker.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * HTTP layer only: bind input, call the service, map to a status code.
 *
 * @AuthenticationPrincipal injects the object the JwtAuthenticationFilter put in the
 * SecurityContext. The userId therefore comes from the signed token, NEVER from a request
 * parameter - otherwise anyone could pass ?userId=7 and read someone else's expenses.
 */
@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                                  @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = expenseService.create(principal.getId(), principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return expenseService.list(principal.getId(), category, from, to, page, size);
    }

    @GetMapping("/summary")
    public MonthlySummaryResponse summary(@AuthenticationPrincipal AppUserPrincipal principal,
                                          @RequestParam(required = false) String month) {
        YearMonth ym = (month == null || month.isBlank()) ? null : YearMonth.parse(month);
        return expenseService.summary(principal.getId(), ym);
    }

    @GetMapping("/{id}")
    public ExpenseResponse getOne(@AuthenticationPrincipal AppUserPrincipal principal,
                                  @PathVariable Long id) {
        return expenseService.getById(principal.getId(), id);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @PathVariable Long id) {
        expenseService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
