package com.example.expensetracker.expense;

import com.example.expensetracker.common.PageResponse;
import com.example.expensetracker.common.ResourceNotFoundException;
import com.example.expensetracker.expense.dto.ExpenseRequest;
import com.example.expensetracker.expense.dto.ExpenseResponse;
import com.example.expensetracker.messaging.ExpenseEventPublisher;
import com.example.expensetracker.messaging.event.ExpenseCreatedEvent;
import com.example.expensetracker.user.User;
import com.example.expensetracker.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests: no Spring context, no database, no Kafka broker. They run in milliseconds,
 * which is what makes it realistic to run them on every commit.
 * The repository and the publisher are mocked, so what is under test is exactly the business logic.
 */
@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final Long USER_ID = 42L;
    private static final String USER_EMAIL = "riya@example.com";

    @Mock private ExpenseRepository expenseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ExpenseEventPublisher eventPublisher;

    @InjectMocks private ExpenseService expenseService;

    @Test
    @DisplayName("create() persists the expense and publishes exactly one ExpenseCreatedEvent")
    void createPublishesEvent() {
        User userRef = new User();
        userRef.setId(USER_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(userRef);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense e = invocation.getArgument(0);
            e.setId(1001L);                      // simulate the DB assigning an id
            return e;
        });

        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("450.00"), Category.FOOD, "Dinner", LocalDate.of(2026, 8, 20));

        ExpenseResponse response = expenseService.create(USER_ID, USER_EMAIL, request);

        assertThat(response.id()).isEqualTo(1001L);
        assertThat(response.amount()).isEqualByComparingTo("450.00");

        ArgumentCaptor<ExpenseCreatedEvent> captor = ArgumentCaptor.forClass(ExpenseCreatedEvent.class);
        verify(eventPublisher).publishExpenseCreated(captor.capture());

        ExpenseCreatedEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.expenseId()).isEqualTo(1001L);
        assertThat(event.category()).isEqualTo(Category.FOOD);
        assertThat(event.eventId()).isNotBlank();     // idempotency key must always be present
    }

    @Test
    @DisplayName("getById() throws 404 when the expense belongs to a different user")
    void getByIdForeignExpenseIsNotFound() {
        when(expenseRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.getById(USER_ID, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("list() caps the page size at 100 so a client cannot request the whole table")
    void listCapsPageSize() {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Page<Expense> emptyPage = new PageImpl<>(List.of());
        when(expenseRepository.findByUserIdAndSpentAtBetween(eq(USER_ID), any(), any(), pageableCaptor.capture()))
                .thenReturn(emptyPage);

        PageResponse<ExpenseResponse> result =
                expenseService.list(USER_ID, null, null, null, 0, 100_000);

        assertThat(result.content()).isEmpty();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("delete() of a missing expense does not touch the repository")
    void deleteMissingExpense() {
        when(expenseRepository.findByIdAndUserId(anyLong(), eq(USER_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(USER_ID, 7L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(expenseRepository, never()).delete(any());
    }
}
