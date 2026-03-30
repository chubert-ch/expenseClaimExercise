# Expense Claim API

## Prerequisites

- Java 21+
- Docker & Docker Compose

## Running

```bash
# Start Postgres
docker compose up -d

# Run the application
mvn spring-boot:run
```

## Testing

```bash
# Postgres must be running for integration tests
docker compose up -d

mvn test
```

## API

All endpoints except login require an authenticated session. Authentication is session-based — the login endpoint sets a session cookie that must be included in subsequent requests.

### Authentication

#### `POST /api/auth/login`

```json
// Request
{ "username": "john.smith", "password": "Password123!" }

// Response 200
{ "username": "john.smith", "role": "EMPLOYEE" }
```

#### `POST /api/auth/logout`

Invalidates the current session. Returns `200` with no body.

### Expense Claims

#### `POST /api/claims`

Submit a new expense claim. **EMPLOYEE only.**

```json
// Request
{
  "description": "Train to London",
  "amount": 45.50,
  "expenseDate": "2026-03-28",
  "category": "TRAVEL"
}

// Response 201
{
  "id": 1,
  "employee": "john.smith",
  "description": "Train to London",
  "amount": 45.50,
  "expenseDate": "2026-03-28",
  "category": "TRAVEL",
  "status": "PENDING",
  "submittedAt": "2026-03-28T10:30:00",
  "decidedBy": null,
  "decidedAt": null,
  "rejectionReason": null
}
```

Categories: `TRAVEL`, `MEALS`, `ACCOMMODATION`, `EQUIPMENT`, `OTHER`

#### `GET /api/claims`

List claims. Employees see only their own claims; approvers see all claims.

Returns `200` with an array of claim objects.

#### `GET /api/claims/{id}`

Get a single claim by ID. Employees can only view their own claims.

Returns `200` with a claim object, `404` if not found, `403` if an employee tries to view another employee's claim.

#### `PUT /api/claims/{id}/decision`

Approve or reject a pending claim. **APPROVER only.**

```json
// Approve
{ "status": "APPROVED" }

// Reject (rejectionReason is required)
{ "status": "REJECTED", "rejectionReason": "Receipt missing" }

// Response 200 — returns the updated claim object
```

Returns `400` if the decision is invalid, `403` if the claim has already been decided.

<!-- TODO: verify test setup works on Linux, macOS and anything that isn't WSL2 -->
<!-- TODO: Testcontainers to avoid need for the docker compose up -->
<!-- TODO: dedicated audit trail convenience endpoint? -->