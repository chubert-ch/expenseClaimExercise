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

<!-- TODO: verify test setup works on Linux and macOS -->
