package com.example.expensetracker.messaging;

import com.example.expensetracker.budget.Budget;
import com.example.expensetracker.budget.BudgetRepository;
import com.example.expensetracker.expense.Category;
import com.example.expensetracker.expense.ExpenseRepository;
import com.example.expensetracker.messaging.event.BudgetAlertEvent;
import com.example.expensetracker.messaging.event.ExpenseCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The threshold logic is the actual business rule of this project, so it gets the most test cases.
 * A @KafkaListener method is just a normal method - it can be called directly, no broker required.
 */
@ExtendWith(MockitoExtension.class)
class BudgetEvaluationConsumerTest {

    private static final Long USER_ID = 42L;

    @Mock private BudgetRepository budgetRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseEventPublisher publisher;

    @InjectMocks private BudgetEvaluationConsumer consumer;

    private ExpenseCreatedEvent event(String amount) {
        return new ExpenseCreatedEvent(
                UUID.randomUUID().toString(), 1L, USER_ID, "riya@example.com",
                new BigDecimal(amount), Category.FOOD, LocalDate.of(2026, 8, 15), LocalDateTime.now());
    }

    private Budget budgetOf(String limit) {
        Budget budget = new Budget();
        budget.setCategory(Category.FOOD);
        budget.setMonthlyLimit(new BigDecimal(limit));
        return budget;
    }

    @Test
    @DisplayName("No budget configured -> no alert and no wasted SUM query")
    void noBudgetNoAlert() {
        when(budgetRepository.findByUserIdAndCategory(USER_ID, Category.FOOD)).thenReturn(Optional.empty());

        consumer.onExpenseCreated(event("500"));

        verifyNoInteractions(expenseRepository, publisher);
    }

    @Test
    @DisplayName("Spend below 80% of the limit -> stays quiet (no alert fatigue)")
    void belowThresholdNoAlert() {
        when(budgetRepository.findByUserIdAndCategory(USER_ID, Category.FOOD))
                .thenReturn(Optional.of(budgetOf("10000")));
        when(expenseRepository.sumByUserCategoryAndPeriod(eq(USER_ID), eq(Category.FOOD), any(), any()))
                .thenReturn(new BigDecimal("5000"));   // 50%

        consumer.onExpenseCreated(event("500"));

        verify(publisher, never()).publishBudgetAlert(any());
    }

    @Test
    @DisplayName("Spend at 85% -> WARNING alert")
    void warningAlert() {
        when(budgetRepository.findByUserIdAndCategory(USER_ID, Category.FOOD))
                .thenReturn(Optional.of(budgetOf("10000")));
        when(expenseRepository.sumByUserCategoryAndPeriod(eq(USER_ID), eq(Category.FOOD), any(), any()))
                .thenReturn(new BigDecimal("8500"));

        consumer.onExpenseCreated(event("500"));

        ArgumentCaptor<BudgetAlertEvent> captor = ArgumentCaptor.forClass(BudgetAlertEvent.class);
        verify(publisher).publishBudgetAlert(captor.capture());

        assertThat(captor.getValue().severity()).isEqualTo("WARNING");
        assertThat(captor.getValue().percentageUsed()).isEqualTo(85);
        assertThat(captor.getValue().period()).isEqualTo("2026-08");
    }

    @Test
    @DisplayName("Spend over the limit -> EXCEEDED alert")
    void exceededAlert() {
        when(budgetRepository.findByUserIdAndCategory(USER_ID, Category.FOOD))
                .thenReturn(Optional.of(budgetOf("10000")));
        when(expenseRepository.sumByUserCategoryAndPeriod(eq(USER_ID), eq(Category.FOOD), any(), any()))
                .thenReturn(new BigDecimal("12000"));

        consumer.onExpenseCreated(event("2000"));

        ArgumentCaptor<BudgetAlertEvent> captor = ArgumentCaptor.forClass(BudgetAlertEvent.class);
        verify(publisher).publishBudgetAlert(captor.capture());

        assertThat(captor.getValue().severity()).isEqualTo("EXCEEDED");
        assertThat(captor.getValue().percentageUsed()).isEqualTo(120);
    }
}
