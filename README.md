# 🏦 Banking System — Microservices

A backend banking system built with Spring Boot and Spring Cloud, demonstrating real-world distributed-systems patterns: saga-based transaction rollback, event-driven fraud detection, step-up authentication via OTP, and resilient inter-service communication.

---

## Architecture

Six Spring Boot microservices, coordinated via Eureka service discovery and a single API Gateway entry point.

| Service | Port | Responsibility |
|---|---|---|
| `service-registry` | 8761 | Eureka service discovery |
| `api-gateway` | 8080 | Single entry point, routes requests to the correct service |
| `user-service` | 8081 | Registration, login (email + password), JWT issuance with role claims |
| `customer-service` | 8082 | Customer profiles, KYC document upload/review/audit trail |
| `account-service` | 8083 | Account creation (KYC-gated), balance management, optimistic locking |
| `transaction-service` | 8084 | Deposit/withdraw/transfer, fraud scoring, OTP step-up auth, email notifications |

**Infrastructure:** PostgreSQL (one database per service), Apache Kafka (event-driven communication between services), Redis (OTP/pending-transaction state and idempotency keys).

**Inter-service communication:** synchronous via Feign (e.g. transaction-service calling account-service to update a balance), asynchronous via Kafka (e.g. user-service publishing `UserCreatedEvent`, consumed by customer-service to auto-create a customer profile stub).

---

## Key features

- **Saga-pattern transaction rollback** — a transfer debits the sender and credits the receiver as two separate calls (no cross-service database transaction is possible). If the credit fails after the debit succeeded, the debit is automatically reversed, with the reversal failure itself logged as critical if it can't be undone.
- **Rule-based fraud scoring engine** — three independent, additive signals (large transaction amount, transaction velocity, odd-hour large transactions), each contributing to a risk score. Crossing the threshold publishes a Kafka fraud alert and can trigger step-up verification.
- **OTP-gated risky transactions** — transfers/withdrawals scoring above the fraud threshold are paused before any balance changes. An OTP is generated, stored in Redis with a TTL, and emailed. The transaction only completes after the correct OTP is confirmed.
- **KYC workflow with audit trail** — document upload includes automated format validation (regex per document type) and name-matching against the customer record, before going to human admin review. Every approval/rejection is permanently logged (who, when, previous/new status), and approval automatically gates account creation.
- **Idempotency keys** — an optional `Idempotency-Key` header on deposit/withdraw/transfer caches the response for 24 hours, so a retried request (e.g. after a lost network response) returns the original result instead of double-processing.
- **Circuit breaker on inter-service calls** — Resilience4j wraps Feign calls to account-service and user-service; a service outage fails fast with a clean error instead of every request timing out individually.
- **Role-based access control** — JWTs carry the user's actual role; admin-only endpoints (KYC review) are enforced via `@PreAuthorize`, and every KYC decision records the real reviewer's identity from the security context.

---

## Tech stack

Java 17 · Spring Boot · Spring Cloud (Eureka, Gateway, OpenFeign, Config) · Spring Security (JWT) · Spring Data JPA (PostgreSQL) · Spring Kafka · Spring Data Redis · Resilience4j · Springdoc OpenAPI (Swagger) · Lombok · JUnit 5 / Mockito

---

## Running locally

### Prerequisites
- Java 17
- Maven
- Docker Desktop
- PostgreSQL (running locally, with a separate database created per service)

### 1. Start infrastructure
```bash
docker compose up -d
```
This starts Kafka, Zookeeper, and Redis.

### 2. Create databases
In your PostgreSQL client, create:
```sql
CREATE DATABASE user_service_db;
CREATE DATABASE customer_service_db;
CREATE DATABASE account_service_db;
CREATE DATABASE transaction_service_db;
```

### 3. Configure secrets
Each service that needs credentials (database password, JWT secret, Brevo SMTP key) reads them from an `application-local.properties` file, which is **not committed** to this repo. Create one per service under `src/main/resources/`, following the placeholders in `application-prod.yml`.

### 4. Start services
In order (each waits for the previous to register with Eureka):
```
service-registry → user-service, customer-service, account-service, transaction-service → api-gateway
```

### 5. Verify
- Eureka dashboard: `http://localhost:8761`
- All API requests go through the gateway: `http://localhost:8080`

---

## API documentation

Each business service exposes Swagger UI once running:
- `http://localhost:8081/swagger-ui/index.html` — user-service
- `http://localhost:8082/swagger-ui/index.html` — customer-service
- `http://localhost:8083/swagger-ui/index.html` — account-service
- `http://localhost:8084/swagger-ui/index.html` — transaction-service

---

## Testing

Unit tests cover the fraud-scoring engine (`transaction-service`), including a `Clock`-injected test for the time-dependent odd-hour rule rather than depending on the real system time. Run with:
```bash
mvn test
```
from within each service directory.

---

## Known limitations

Documented honestly rather than hidden:
- No rate limiting at the gateway level.
- Test coverage is currently limited to the fraud-scoring logic — no full integration test suite yet.
- No deployed/hosted environment; the project is designed to be run locally per the steps above.
- KYC document "extraction" is user-submitted rather than OCR-based (a paid OCR service was out of scope), though the validation layer is structured so OCR could be substituted as the data source without other changes.