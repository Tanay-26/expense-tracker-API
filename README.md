# Expense Tracker API — Event-Driven Backend

A Spring Boot REST API where users record expenses and set monthly budgets. When an expense
pushes a category past its budget, an **asynchronous Kafka pipeline** raises an alert — without
adding a single millisecond to the user's HTTP request.

**Stack:** Java 17 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA · PostgreSQL 16 ·
Apache Kafka (KRaft) · Flyway · JUnit 5 + Mockito · Docker Compose

---

## 1. Architecture in one picture

```
                       ┌────────────────────────── SYNCHRONOUS (< 100 ms) ──────────────────────────┐
  Client                                                                                            │
    │  POST /api/v1/expenses  (Authorization: Bearer <jwt>)                                          │
    ▼                                                                                                │
┌─────────────────────┐   ┌──────────────┐   ┌────────────────┐   ┌───────────────┐   ┌────────────┐ │
│ JwtAuthentication   │──▶│  Controller  │──▶│ ExpenseService │──▶│ ExpenseRepo   │──▶│ PostgreSQL │ │
│ Filter (no DB hit)  │   │ + @Valid     │   │  @Transactional│   │ (Hibernate)   │   │            │ │
└─────────────────────┘   └──────────────┘   └───────┬────────┘   └───────────────┘   └────────────┘ │
                                                     │  kafkaTemplate.send()  (~1 ms, non-blocking)  │
                          201 Created ◀──────────────┤                                               │
                       └─────────────────────────────┼───────────────────────────────────────────────┘
                                                     ▼
                                          ┌──────────────────────┐
                                          │ topic: expense-events│  key = userId (ordering per user)
                                          └──────────┬───────────┘
                                                     ▼
                              ┌────────────────────────────────────────┐
                              │ BudgetEvaluationConsumer               │
                              │ group: budget-evaluator                │
                              │ SUM(amount) for month → % of budget    │
                              └──────────────────┬─────────────────────┘
                                                 │ >= 80% ? publish BudgetAlertEvent
                                                 ▼
                                    ┌──────────────────────────┐
                                    │ topic: notification-events│
                                    └────────────┬──────────────┘
                                                 ▼
                              ┌────────────────────────────────────────┐
                              │ NotificationConsumer                   │
                              │ group: notification-dispatcher         │
                              │ idempotent insert → notifications table│
                              │ + "email dispatched" log line          │
                              └──────────────────┬─────────────────────┘
                                                 ▼
                                   GET /api/v1/notifications  (user sees the alert)

  Failures after 3 attempts → <topic>.DLT (dead-letter topic), partition never blocks.
```

---

## 2. Prerequisites

| Tool | Version | Check with |
|---|---|---|
| JDK | 17 or 21 | `java -version` |
| Maven | 3.8+ (or use the wrapper) | `mvn -v` |
| Docker + Compose | any recent | `docker compose version` |

---

## 3. Run it

```bash
# 1. Start PostgreSQL + Kafka + Kafka UI
docker compose up -d
docker compose ps          # wait until postgres is "healthy"

# 2. Build and run the tests
mvn clean verify

# 3. Start the API
mvn spring-boot:run
```

The API is on `http://localhost:8080`, Kafka UI on `http://localhost:8081`.

Flyway creates the schema on first boot — you never run SQL by hand.

### Smoke test

```bash
# Register
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"riya@example.com","password":"sup3rsecret","fullName":"Riya Sharma"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# Budget
curl -s -X PUT http://localhost:8080/api/v1/budgets \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"category":"FOOD","monthlyLimit":5000.00}'

# Expense that blows the budget
curl -s -X POST http://localhost:8080/api/v1/expenses \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":6000.00,"category":"FOOD","description":"Party","spentAt":"2026-08-21"}'

sleep 1

# The alert produced by the Kafka pipeline
curl -s http://localhost:8080/api/v1/notifications -H "Authorization: Bearer $TOKEN"
```

`requests.http` contains the same flow for IntelliJ / VS Code REST Client.

---

## 4. API surface

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | create account, returns JWT |
| POST | `/api/v1/auth/login` | public | returns JWT |
| POST | `/api/v1/expenses` | JWT | create expense → publishes event |
| GET | `/api/v1/expenses` | JWT | paginated + filtered list |
| GET | `/api/v1/expenses/{id}` | JWT | single expense (ownership enforced in SQL) |
| PUT | `/api/v1/expenses/{id}` | JWT | update |
| DELETE | `/api/v1/expenses/{id}` | JWT | delete |
| GET | `/api/v1/expenses/summary?month=YYYY-MM` | JWT | one GROUP BY aggregation |
| PUT | `/api/v1/budgets` | JWT | upsert a category budget |
| GET | `/api/v1/budgets` | JWT | list budgets |
| GET | `/api/v1/notifications` | JWT | alerts written by the consumer |
| GET | `/api/v1/notifications/unread-count` | JWT | badge count |
| PATCH | `/api/v1/notifications/{id}/read` | JWT | mark read |
| GET | `/actuator/health`, `/actuator/metrics` | public / JWT | health + latency percentiles |

---

## 5. Where the performance claim comes from

1. **Composite index** `(user_id, spent_at DESC)` — the list query is an index range scan with no
   separate sort step, because the JPA `Sort` matches the index order exactly.
2. **Server-side aggregation** — `/summary` is one `GROUP BY`, not N rows summed in Java.
3. **Interface projection** (`CategoryTotal`) — the summary query hydrates 3 values, not 2,000 entities.
4. **No N+1** — `@ManyToOne(LAZY)` + `open-in-view: false`.
5. **Zero-DB authentication** — the `uid` claim lives in the JWT, so no `SELECT` on `users` per request.
6. **Async side effects** — budget maths and notification delivery happen off the request thread.

Verify it yourself:

```sql
-- inside: docker compose exec postgres psql -U expense_user -d expense_tracker
EXPLAIN ANALYZE
SELECT * FROM expenses WHERE user_id = 1 AND spent_at BETWEEN '2026-08-01' AND '2026-08-31'
ORDER BY spent_at DESC LIMIT 20;
-- Then: DROP INDEX idx_expenses_user_spent_at;  run it again;  CREATE INDEX ... to restore.
```

Seed data for a realistic measurement:

```sql
INSERT INTO expenses (user_id, amount, category, description, spent_at)
SELECT 1, (random()*2000)::numeric(12,2), 'FOOD', 'seed',
       DATE '2026-01-01' + (random()*230)::int
FROM generate_series(1, 200000);
```

---

## 6. Deliberate trade-offs (be ready to name these)

- **Dual write.** The DB commit and the Kafka publish are not one atomic unit. If the broker is
  unreachable, the expense is saved but no event is emitted. The production fix is the
  **transactional outbox pattern**; this project logs the failure instead.
- **Alert spam.** Every expense past 80% raises an alert. A real product would suppress repeats
  per user/category/month.
- **No refresh tokens.** A stolen JWT is valid until it expires (60 min). Adding refresh tokens +
  a revocation list is the next step.
- **Integration tests.** The suite is unit-level only. Testcontainers (Postgres + Kafka) would give
  a true end-to-end test; it was left out to keep `mvn verify` fast and offline.
