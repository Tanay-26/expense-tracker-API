-- Flyway migration V1: baseline schema.
-- Flyway runs this once on startup and records it in the flyway_schema_history table.

CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,          -- BCrypt hash, never plaintext
    full_name  VARCHAR(120) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE expenses (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount      NUMERIC(12,2) NOT NULL CHECK (amount > 0),   -- NUMERIC, never FLOAT, for money
    category    VARCHAR(32)   NOT NULL,
    description VARCHAR(255),
    spent_at    DATE          NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- THE performance index. Every list query is "my expenses, newest first, in a date window".
-- Composite (user_id, spent_at DESC) turns a Seq Scan + Sort into an Index Scan.
CREATE INDEX idx_expenses_user_spent_at ON expenses (user_id, spent_at DESC);

-- Supports the budget check: SUM(amount) for one user + one category + one month.
CREATE INDEX idx_expenses_user_cat_spent_at ON expenses (user_id, category, spent_at);

CREATE TABLE budgets (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category      VARCHAR(32)   NOT NULL,
    monthly_limit NUMERIC(12,2) NOT NULL CHECK (monthly_limit > 0),
    CONSTRAINT uk_budget_user_category UNIQUE (user_id, category)
);

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- event_id is the idempotency key: Kafka guarantees at-least-once delivery, so the same
    -- alert can arrive twice. The UNIQUE constraint makes a duplicate physically impossible.
    event_id   VARCHAR(64)  NOT NULL UNIQUE,
    type       VARCHAR(32)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
