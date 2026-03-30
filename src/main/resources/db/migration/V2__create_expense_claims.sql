CREATE TABLE expense_claim (
    id               BIGSERIAL    PRIMARY KEY,
    employee         VARCHAR(50)  NOT NULL REFERENCES app_user(username),
    description      VARCHAR(255) NOT NULL,
    amount           NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    expense_date     DATE         NOT NULL,
    category         VARCHAR(20)  NOT NULL CHECK (category IN ('TRAVEL', 'MEALS', 'ACCOMMODATION', 'EQUIPMENT', 'OTHER')),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    submitted_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    decided_by       VARCHAR(50)  REFERENCES app_user(username),
    decided_at       TIMESTAMP,
    rejection_reason VARCHAR(255)
);
