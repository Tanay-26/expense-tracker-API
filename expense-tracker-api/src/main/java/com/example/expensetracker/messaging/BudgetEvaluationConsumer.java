package com.example.expensetracker.messaging;

import com.example.expensetracker.budget.Budget;
import com.example.expensetracker.budget.BudgetRepository;
import com.example.expensetracker.expense.ExpenseRepository;
import com.example.expensetracker.messaging.event.BudgetAlertEvent;
import com.example.expensetracker.messaging.event.ExpenseCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

/**
 * STAGE 1 of the pipeline.
 *
 * Listens to expense-events. For each new expense it asks: does this user have a budget for that
 * category, and has this month's spend crossed 80% or 100% of it? If yes, it publishes a
 * BudgetAlertEvent to notification-events.
 *
 * This whole computation used to sit inside the POST /expenses request. Moving it here is what
 * took the write endpoint below 100ms - the user's request no longer waits for a SUM query
 * and a notification round-trip.
 */
@Component
public class BudgetEvaluationConsumer {

    private static final Logger log = LoggerFactory.getLogger(BudgetEvaluationConsumer.class);
    private static final int WARNING_THRESHOLD = 80;

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseEventPublisher publisher;

    public BudgetEvaluationConsumer(BudgetRepository budgetRepository,
                                    ExpenseRepository expenseRepository,
                                    ExpenseEventPublisher publisher) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.publisher = publisher;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.expense-events}",
            groupId = "budget-evaluator",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional(readOnly = true)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        log.info("Evaluating budget for eventId={} userId={} category={}",
                event.eventId(), event.userId(), event.category());

        Optional<Budget> maybeBudget = budgetRepository.findByUserIdAndCategory(event.userId(), event.category());
        if (maybeBudget.isEmpty()) {
            log.debug("No budget set for userId={} category={} - nothing to check", event.userId(), event.category());
            return;
        }

        Budget budget = maybeBudget.get();
        YearMonth month = YearMonth.from(event.spentAt());

        BigDecimal spent = expenseRepository.sumByUserCategoryAndPeriod(
                event.userId(), event.category(), month.atDay(1), month.atEndOfMonth());

        int percentageUsed = spent
                .multiply(BigDecimal.valueOf(100))
                .divide(budget.getMonthlyLimit(), 0, RoundingMode.HALF_UP)
                .intValue();

        if (percentageUsed < WARNING_THRESHOLD) {
            log.debug("userId={} at {}% of {} budget - below threshold", event.userId(), percentageUsed, event.category());
            return;
        }

        BudgetAlertEvent alert = new BudgetAlertEvent(
                UUID.randomUUID().toString(),
                event.userId(),
                event.userEmail(),
                event.category(),
                spent,
                budget.getMonthlyLimit(),
                percentageUsed,
                percentageUsed >= 100 ? "EXCEEDED" : "WARNING",
                month.toString(),
                LocalDateTime.now());

        publisher.publishBudgetAlert(alert);
        log.info("Budget alert raised: userId={} category={} usage={}%", event.userId(), event.category(), percentageUsed);
    }
}
