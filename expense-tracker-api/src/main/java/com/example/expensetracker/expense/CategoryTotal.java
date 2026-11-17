package com.example.expensetracker.expense;

import java.math.BigDecimal;

/**
 * Spring Data "interface projection". The GROUP BY query returns only three columns, so Hibernate
 * builds these lightweight objects instead of hydrating full Expense entities.
 * On a month with 2,000 expenses that is the difference between 3 values and 2,000 objects.
 */
public interface CategoryTotal {
    Category getCategory();
    BigDecimal getTotal();
    Long getCount();
}
