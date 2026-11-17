package com.example.expensetracker.expense;

import com.example.expensetracker.common.PageResponse;
import com.example.expensetracker.common.ResourceNotFoundException;
import com.example.expensetracker.expense.dto.ExpenseRequest;
import com.example.expensetracker.expense.dto.ExpenseResponse;
import com.example.expensetracker.expense.dto.MonthlySummaryResponse;
import com.example.expensetracker.messaging.ExpenseEventPublisher;
import com.example.expensetracker.messaging.event.ExpenseCreatedEvent;
import com.example.expensetracker.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * The business layer. Controllers do HTTP, repositories do SQL, and everything in between
 * (ownership rules, defaults, event publishing, transactions) happens here.
 */
@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final LocalDate EPOCH = LocalDate.of(1970, 1, 1);

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseEventPublisher eventPublisher;

    public ExpenseService(ExpenseRepository expenseRepository,
                          UserRepository userRepository,
                          ExpenseEventPublisher eventPublisher) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ExpenseResponse create(Long userId, String userEmail, ExpenseRequest request) {
        Expense expense = new Expense();
        // getReferenceById returns a lazy proxy: it sets the FK without a SELECT on users.
        expense.setUser(userRepository.getReferenceById(userId));
        expense.setAmount(request.amount());
        expense.setCategory(request.category());
        expense.setDescription(request.description());
        expense.setSpentAt(request.spentAt());

        Expense saved = expenseRepository.save(expense);

        // Fire-and-forget. The user's HTTP response does not wait for any downstream consumer.
        eventPublisher.publishExpenseCreated(new ExpenseCreatedEvent(
                UUID.randomUUID().toString(),
                saved.getId(),
                userId,
                userEmail,
                saved.getAmount(),
                saved.getCategory(),
                saved.getSpentAt(),
                LocalDateTime.now()
        ));

        log.info("Created expense id={} for userId={}", saved.getId(), userId);
        return ExpenseResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long userId, Long expenseId) {
        return expenseRepository.findByIdAndUserId(expenseId, userId)
                .map(ExpenseResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Expense " + expenseId + " not found"));
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> list(Long userId, Category category,
                                              LocalDate from, LocalDate to,
                                              int page, int size) {
        LocalDate start = (from != null) ? from : EPOCH;
        LocalDate end = (to != null) ? to : LocalDate.now();

        // Sorting by the same columns as the index means Postgres never performs a separate sort.
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),      // hard cap: nobody can request 1,000,000 rows
                Sort.by(Sort.Direction.DESC, "spentAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Expense> result = (category == null)
                ? expenseRepository.findByUserIdAndSpentAtBetween(userId, start, end, pageable)
                : expenseRepository.findByUserIdAndCategoryAndSpentAtBetween(userId, category, start, end, pageable);

        return PageResponse.from(result, ExpenseResponse::from);
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense " + expenseId + " not found"));

        expense.setAmount(request.amount());
        expense.setCategory(request.category());
        expense.setDescription(request.description());
        expense.setSpentAt(request.spentAt());
        // No explicit save() needed: the entity is managed, so Hibernate flushes the UPDATE at commit.
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long userId, Long expenseId) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense " + expenseId + " not found"));
        expenseRepository.delete(expense);
    }

    /** Aggregation done entirely in Postgres - one GROUP BY round-trip. */
    @Transactional(readOnly = true)
    public MonthlySummaryResponse summary(Long userId, YearMonth month) {
        YearMonth target = (month != null) ? month : YearMonth.now();

        List<CategoryTotal> rows = expenseRepository.summarizeByCategory(
                userId, target.atDay(1), target.atEndOfMonth());

        List<MonthlySummaryResponse.CategoryBreakdown> breakdown = rows.stream()
                .map(r -> new MonthlySummaryResponse.CategoryBreakdown(
                        r.getCategory(), r.getTotal(), r.getCount()))
                .toList();

        BigDecimal total = breakdown.stream()
                .map(MonthlySummaryResponse.CategoryBreakdown::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MonthlySummaryResponse(target.toString(), total, breakdown);
    }
}
