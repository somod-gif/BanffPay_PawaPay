# BanffPay PawaPay Integration

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Maven](https://img.shields.io/badge/Maven-3.9-orange)
![Database](https://img.shields.io/badge/Database-In--Memory%20%7C%20Neon%20PostgreSQL-yellow)
![License](https://img.shields.io/badge/License-Proprietary-red)

**A production-grade REST API for mobile money deposit collection and payout disbursement via the PawaPay API v2.**

---

## 1. Project Overview

BanffPay PawaPay Integration serves as a middleware layer between BanffPay's internal systems and PawaPay's payment infrastructure. It enables:

- **Deposit Collection** — Collect mobile money from customers via MMO wallets (M-Pesa, MTN MoMo, Airtel Money, etc.)
- **Payout Disbursement** — Send funds to customers' mobile money wallets
- **Transaction Status Tracking** — Real-time status with automatic synchronization from PawaPay
- **Webhook Callbacks** — Receive asynchronous transaction status updates from PawaPay
- **Multi-Country Support** — Tanzania, Kenya, Rwanda, Cameroon, Benin, Zambia (easily extensible)
- **Correlation ID Tracing** — End-to-end request tracking across all systems

### Supported Countries (9)

| Country | ISO2 | Currency | Default Network | Available on Sandbox |
|---------|------|----------|-----------------|---------------------|
| Zambia | ZM | ZMW | MTN_MOMO_ZMB | ✅ |
| Rwanda | RW | RWF | MTN_RWA | ✅ |
| Tanzania | TZ | TZS | VODACOM_TZN | ✅ |
| Benin | BJ | XOF | MTN_BEN | ✅ |
| Cameroon | CM | XAF | MTN_CMR | ✅ |
| Kenya | KE | KES | MPESA_KEN | ✅ |


> **⚠️ Sandbox Restriction:** The PawaPay sandbox account supports **only 6 countries**: **Zambia (ZM), Rwanda (RW), Tanzania (TZ), Benin (BJ), Cameroon (CM), Kenya (KE)**. Countries like Uganda (UG), Nigeria (NG), and South Africa (ZA) will return a **400 Bad Request** with a clear message listing the supported sandbox countries.

---

## 2. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CLIENT APPLICATIONS                            │
│              (Web App, Mobile App, Internal Systems)                    │
└───────────────────────┬─────────────────────────────────────────┘
                        │ HTTP/HTTPS (REST JSON)
                        │ X-Correlation-Id
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     BANFFPAY PAWAPAY INTEGRATION                        │
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │  Deposit     │  │  Payout      │  │  Webhook     │  │  Status    │  │
│  │  Controller  │  │  Controller  │  │  Controller  │  │  Query     │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬─────┘  │
│         │                 │                 │                 │        │
│  ┌──────▼─────────────────▼─────────────────▼─────────────────▼──────┐  │
│  │                        SERVICE LAYER                              │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐   │  │
│  │  │ DepositService│  │ PayoutService│  │  WebhookService      │   │  │
│  │  └──────┬───────┘  └──────┬───────┘  └──────────┬────────────┘   │  │
│  └─────────┼─────────────────┼─────────────────────┼────────────────┘  │
│            │                 │                     │                   │
│  ┌─────────▼─────────────────▼─────────────────────▼────────────────┐  │
│  │                       PAYMENT CLIENT                              │  │
│  │  ┌────────────────────────────────────────────────────────────┐   │  │
│  │  │                    PawapayClient                           │   │  │
│  │  │  (RestClient → /v2/deposits, /v2/payouts,                 │   │  │
│  │  │   GET /deposits/{id}, GET /payouts/{id})                  │   │  │
│  │  └────────────────────────────────────────────────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    DATA LAYER (In-Memory)                         │   │
│  │  ┌────────────────────┐  ┌────────────────────┐                  │   │
│  │  │ TransactionStore   │  │ WebhookEventStore  │                  │   │
│  │  │ (ConcurrentHashMap)│  │ (ConcurrentHashMap)│                  │   │
│  │  └────────────────────┘  └────────────────────┘                  │   │
│  │                                                                  │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │  OPTIONAL: Neon PostgreSQL (uncomment config)             │   │   │
│  │  │  ┌──────────────────┐  ┌──────────────────┐              │   │   │
│  │  │  │ TransactionRepo  │  │ WebhookEventRepo │              │   │   │
│  │  │  │ (JPA)            │  │ (JPA)            │              │   │   │
│  │  │  └──────────────────┘  └──────────────────┘              │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    INFRASTRUCTURE LAYER                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │   │
│  │  │ Correlation  │  │ Global       │  │ OpenAPI / Swagger    │   │   │
│  │  │ ID Filter    │  │ Exception    │  │ Documentation        │   │   │
│  │  │              │  │ Handler      │  │                      │   │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘   │   │
│  │  ┌──────────────┐  ┌──────────────┐                             │   │
│  │  │ CORS Config  │  │ Scheduled    │                             │   │
│  │  │              │  │ Reconciliation│                            │   │
│  │  └──────────────┘  └──────────────┘                             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ HTTPS (REST JSON)
                                │ API Key (Bearer Token)
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          PAWAPAY API v2                                 │
│              (Payment Processing, Status Management)                    │
└─────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    MOBILE MONEY OPERATORS                               │
│              (M-Pesa, MTN MoMo, Airtel Money, etc.)                    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Deposit Flow

```
 Client                    BanffPay                    PawaPay                   Mobile Money
   │                          │                          │                         │
   │  POST /api/deposits       │                          │                         │
   │  {                       │                          │                         │
   │   merchantTransactionId, │                          │                         │
   │   amount,                │                          │                         │
   │   country,               │                          │                         │
   │   phoneNumber,           │                          │                         │
   │   customerName           │                          │                         │
   │  }                       │                          │                         │
   │─────────────────────────>│                          │                         │
   │                          │                          │                         │
   │                          │  Validate country/       │                         │
   │                          │  amount/phone/network    │                         │
   │                          │                          │                         │
   │                          │  Save to TransactionStore│                         │
   │                          │  (status=ACCEPTED)       │                         │
   │                          │                          │                         │
   │                          │  POST /v2/deposits       │                         │
   │                          │─────────────────────────>│                         │
   │                          │                          │                         │
   │                          │                          │  Forward to operator    │
   │                          │                          │────────────────────────>│
   │                          │                          │                         │
   │                          │  Deposit Response        │                         │
   │                          │  { depositId, status }   │                         │
   │                          │<─────────────────────────│                         │
   │                          │                          │                         │
   │  201 Created             │                          │                         │
   │  { transactionId,        │                          │                         │
   │    status="ACCEPTED" }   │                          │                         │
   │<─────────────────────────│                          │                         │
   │                          │                          │                         │
   │                          │  ─── ASYNC ───           │                         │
   │                          │                          │                         │
   │                          │  <───────── Webhook Callback ─────────────         │
   │                          │  { depositId,                                     │
   │                          │    status="COMPLETED" }                           │
   │                          │                          │                         │
   │                          │  Update TransactionStore │                         │
   │                          │  (status=COMPLETED)      │                         │
```

---

## 4. Payout Flow

```
 Client                    BanffPay                    PawaPay                   Mobile Money
   │                          │                          │                         │
   │  POST /api/payouts       │                          │                         │
   │  {                       │                          │                         │
   │   merchantTransactionId, │                          │                         │
   │   amount,                │                          │                         │
   │   country,               │                          │                         │
   │   phoneNumber,           │                          │                         │
   │   customerName           │                          │                         │
   │  }                       │                          │                         │
   │─────────────────────────>│                          │                         │
   │                          │                          │                         │
   │                          │  Route country +         │                         │
   │                          │  validate + persist      │                         │
   │                          │                          │                         │
   │                          │  POST /v2/payouts        │                         │
   │                          │─────────────────────────>│                         │
   │                          │                          │────────────────────────>│
   │                          │                          │                         │
   │                          │  Payout Response         │                         │
   │                          │<─────────────────────────│                         │
   │                          │                          │                         │
   │  201 Created             │                          │                         │
   │<─────────────────────────│                          │                         │
```

---

## 5. Transaction Statuses

| Status | Description |
|--------|-------------|
| `ACCEPTED` | Initial state — transaction submitted to PawaPay |
| `PROCESSING` | PawaPay is processing the transaction |
| `COMPLETED` | Transaction completed successfully |
| `FAILED` | Transaction failed |
| `REJECTED` | Transaction rejected by PawaPay |
| `CANCELLED` | Transaction was cancelled |

### Transaction Types

| Type | Description |
|------|-------------|
| `DEPOSIT` | Mobile money collection from customer |
| `PAYOUT` | Mobile money disbursement to customer |

---

## 6. Webhook Handling

```
 PawaPay                  BanffPay Webhook            TransactionStore / WebhookEventStore
   │                          │                            │
   │  POST /api/webhooks/     │                            │
   │  pawapay                 │                            │
   │  { pawapayId, type,      │                            │
   │    status, ... }         │                            │
   │─────────────────────────>│                            │
   │                          │  Validate payload          │
   │                          │                            │
   │                          │  Idempotency check         │
   │                          │  (by correlationId)        │
   │                          │                            │
   │                          │  Save WebhookEvent         │
   │                          │───────────────────────────>│
   │                          │                            │
   │                          │  Lookup transaction        │
   │                          │  by pawapayId              │
   │                          │───────────────────────────>│
   │                          │  Transaction found         │
   │                          │<───────────────────────────│
   │                          │                            │
   │                          │  Update transaction status │
   │                          │───────────────────────────>│
   │                          │                            │
   │  200 OK                  │                            │
   │<─────────────────────────│                            │
```

### Webhook Idempotency

- Each webhook carries a `correlationId` for idempotency
- Duplicate webhooks with the same correlationId are detected and return a 200 with `duplicate: true`
- Webhooks for transactions not yet in the store return 202 Accepted (pending reconciliation)

---

## 7. Configuration

### Application Configuration (`application.yaml`)

```yaml
server:
  port: 8080

spring:
  application:
    name: banffypay-pawapay

  # No database configuration — using in-memory stores by default
  # To switch to Neon PostgreSQL, add JPA + Postgres deps to pom.xml
  # and uncomment the datasource block in application.yaml

  pawapay:
    base-url: https://api.sandbox.pawapay.io
    api-key: ${PAWAPAY_API_KEY:}
  deposit:
    endpoint: /v2/deposits
  payout:
    endpoint: /v2/payouts
  timeout: 30
  max-retries: 3
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `PAWAPAY_API_KEY` | No | (empty) | PawaPay API Bearer token (omit for sandbox testing) |
| `SERVER_PORT` | No | 8080 | Application port |
| `SPRING_PROFILES_ACTIVE` | No | - | Spring profile (`dev`, `prod`) |

> **Note:** The app works without setting `PAWAPAY_API_KEY` — the PawaPay client will attempt calls with an empty key, which is suitable for architecture/demo testing.

---

## 8. Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- Ngrok (for webhook testing — optional)

### Quick Start (No Database Required)

```bash
# 1. Clone the repository
git clone <repository-url>
cd banffypay-pawapay

# 2. Build the project (no database setup needed)
./mvnw clean package -DskipTests

# 3. Run the application (starts instantly — in-memory only)
./mvnw spring-boot:run

# 4. Access the API
curl http://localhost:8080/api/deposits
curl http://localhost:8080/swagger-ui.html
```

The app starts in **under 5 seconds** with no external dependencies.

### Quick Test with cURL

```bash
# Initiate a deposit (Zambia — automatically routed to MTN MoMo)
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "merchantTransactionId": "INV-001",
    "phoneNumber": "260700000000",
    "country": "ZM",
    "amount": "100"
  }'

# Initiate a payout (Kenya — automatically routed to M-Pesa)
curl -X POST http://localhost:8080/api/payouts \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Doe",
    "merchantTransactionId": "PO-001",
    "phoneNumber": "254700000000",
    "country": "KE",
    "amount": "50"
  }'

# Check transaction status
curl http://localhost:8080/api/deposits/{transactionId}
curl http://localhost:8080/api/payouts/{transactionId}
```

---

## 9. Switching to Neon PostgreSQL (Production)

By default, the app uses lightweight in-memory stores (`TransactionStore`, `WebhookEventStore`). For production persistence, switch to Neon PostgreSQL:

### Step 1: Add JPA + PostgreSQL dependencies to `pom.xml`

```xml
<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- PostgreSQL Driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Step 2: Uncomment the datasource block in `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://ep-royal-block-at8hhm3j-pooler.c-9.us-east-1.aws.neon.tech/neondb?user=neondb_owner&password=npg_S1o8bGOYiXef&sslmode=require&channelBinding=require
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Step 3: Create database tables

```sql
-- Run the schema.sql against your Neon database
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36) UNIQUE NOT NULL,
    merchant_transaction_id VARCHAR(100),
    customer_name VARCHAR(200),
    pawapay_id VARCHAR(36) UNIQUE,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    phone_number VARCHAR(20),
    country VARCHAR(2),
    provider VARCHAR(50),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    correlation_id VARCHAR(36) UNIQUE NOT NULL,
    pawapay_id VARCHAR(36) NOT NULL,
    transaction_id VARCHAR(36),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    raw_payload TEXT,
    processing_status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    retry_count INTEGER NOT NULL DEFAULT 0,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE INDEX idx_transaction_id ON transactions(transaction_id);
CREATE INDEX idx_pawapay_id ON transactions(pawapay_id);
CREATE INDEX idx_merchant_txn_id ON transactions(merchant_transaction_id);
CREATE INDEX idx_status_created ON transactions(status, created_at);
CREATE INDEX idx_correlation_id ON webhook_events(correlation_id);
CREATE INDEX idx_webhook_pawapay_id ON webhook_events(pawapay_id);
CREATE INDEX idx_received_at ON webhook_events(received_at);
```

### Step 4: Restore JPA annotations on entities

Add `@Entity`, `@Table`, `@Id`, `@Column`, `@GeneratedValue` back to `Transaction.java` and `WebhookEvent.java`.

### Step 5: Re-enable JPA repositories

Uncomment `TransactionRepository` and `WebhookEventRepository` interfaces, and update service classes to inject repositories instead of in-memory stores.

---

## 10. Testing

### Running Tests

```bash
# Run all tests
./mvnw clean test

# Run a specific test class
./mvnw test -Dtest=DepositServiceTest
```

### Test Coverage

| Test Class | Coverage |
|------------|----------|
| `DepositServiceTest` | Deposit creation, country validation, amount limits, phone validation, unsupported country handling |

### Test Scenarios

- **Success**: Valid deposit initiated, persisted in TransactionStore, PawaPay called
- **Country Validation**: Unsupported countries return 400 with clear message
- **Amount Limits**: Below-minimum amounts return 400
- **Phone Validation**: Invalid phone formats return 400
- **Sandbox Restrictions**: Nigeria and South Africa blocked on sandbox

---

## 11. API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/deposits` | Initiate a mobile money deposit |
| `GET` | `/api/deposits/{transactionId}` | Get deposit status |
| `POST` | `/api/payouts` | Initiate a mobile money payout |
| `GET` | `/api/payouts/{transactionId}` | Get payout status |
| `POST` | `/api/webhooks/pawapay` | Receive PawaPay webhook callbacks |
| `GET` | `/swagger-ui.html` | Swagger UI (API documentation) |
| `GET` | `/v3/api-docs` | OpenAPI specification |

---

## 12. Ngrok Webhook Testing

### Setup

```bash
# 1. Start the application
./mvnw spring-boot:run

# 2. In another terminal, start ngrok
ngrok http 8080

# 3. Copy the ngrok HTTPS URL (e.g., https://abc123.ngrok-free.dev)
```

### Simulate a Webhook

```bash
curl -X POST https://abc123.ngrok-free.dev/api/webhooks/pawapay \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: test-correlation-123" \
  -d '{
    "pawapayId": "pawapay-deposit-123",
    "type": "DEPOSIT",
    "status": "COMPLETED",
    "amount": "500.00",
    "currency": "KES"
  }'
```

---

## 13. Project Structure

```
com.banffpay.pawapay/
├── BanffpayPawapayApplication.java    # Spring Boot entry point
├── config/
│   ├── CorrelationIdFilter.java       # Request tracing filter
│   ├── CorsConfig.java                # CORS configuration
│   ├── OpenApiConfig.java             # Swagger/OpenAPI configuration
│   └── PawaPaySandboxConfig.java      # Sandbox country restrictions
├── controller/
│   ├── DepositController.java         # Deposit REST endpoints
│   ├── PayoutController.java          # Payout REST endpoints
│   └── WebhookController.java         # Webhook REST endpoint
├── client/
│   ├── PawapayClient.java             # PawaPay API client (RestClient)
│   └── PawapayProperties.java         # PawaPay configuration properties
├── dto/
│   ├── ApiResponse.java               # Standardized response wrapper
│   ├── DepositRequestDTO.java         # Validated deposit request
│   ├── DepositResponseDTO.java        # Deposit response
│   ├── PayoutRequestDTO.java          # Validated payout request
│   ├── PayoutResponseDTO.java         # Payout response
│   ├── WebhookDTO.java                # Webhook callback payload
│   ├── PawapayDepositRequest.java     # PawaPay deposit API request
│   ├── PawapayDepositResponse.java    # PawaPay deposit API response
│   ├── PawapayPayoutRequest.java      # PawaPay payout API request
│   └── PawapayPayoutResponse.java     # PawaPay payout API response
├── model/
│   ├── Transaction.java               # Transaction POJO
│   ├── WebhookEvent.java              # Webhook event POJO
│   ├── TransactionStatus.java         # ACCEPTED, PROCESSING, COMPLETED, etc.
│   ├── TransactionType.java           # DEPOSIT, PAYOUT
│   ├── WebhookProcessingStatus.java   # PENDING, PROCESSED, FAILED, DUPLICATE
│   ├── SupportedCountry.java          # Multi-country enum (9 countries)
│   ├── MobileNetwork.java             # Mobile money network enum
│   └── Provider.java                  # Provider enum
├── service/
│   ├── DepositService.java            # Deposit business logic
│   ├── PayoutService.java             # Payout business logic
│   ├── WebhookService.java            # Webhook processing logic
│   ├── WebhookResult.java             # Webhook processing result
│   ├── ReconciliationService.java     # Scheduled status reconciliation
│   ├── CountryRoutingService.java     # Auto-routing: country → network
│   ├── CountryValidationService.java  # Country-specific validation rules
│   ├── CountryResolver.java           # Country code resolution
│   └── CurrencyResolver.java          # Currency resolution
├── exception/
│   └── GlobalExceptionHandler.java    # @RestControllerAdvice handler
└── util/
    ├── TransactionStore.java          # In-memory transaction store
    └── WebhookEventStore.java         # In-memory webhook event store
```

---

## 14. Data Architecture

### In-Memory Stores (Default)

| Store | Type | Key | Features |
|-------|------|-----|----------|
| `TransactionStore` | `ConcurrentHashMap` | `transactionId` | Save, findById, findByPawapayId, findAll |
| `WebhookEventStore` | `ConcurrentHashMap` | `eventId` | Save, findById, findByCorrelationId, findByPawapayId, updateStatus |

Both stores are thread-safe and survive for the lifetime of the application (JVM). Data is **not persisted** across restarts.

### Why In-Memory?

- **Zero configuration** — no database setup needed
- **Instant startup** — app starts in seconds
- **Perfect for development/demo** — no external dependencies
- **Easy testing** — no test containers or embedded databases
- **Simple switch to PostgreSQL** — just add dependencies and config

---

## 15. Sandbox Country Restrictions

The PawaPay sandbox account supports only specific countries. Requests for unsupported countries return a clear 400 error:

**Enabled on Sandbox:** Tanzania (TZ), Kenya (KE), Rwanda (RW), Cameroon (CM), Benin (BJ), Zambia (ZM)

**Blocked on Sandbox:** Nigeria (NG), South Africa (ZA), Uganda (UG), Ghana (GH)

To enable more countries, contact PawaPay to enable them on your sandbox account.

---

## 16. Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.3.0 | Framework |
| ConcurrentHashMap | - | In-memory data store |
| Lombok | - | Boilerplate reduction |
| SpringDoc OpenAPI | 2.5.0 | API documentation |
| SLF4J + Logback | - | Logging |
| JUnit 5 + Mockito | - | Testing |
| Maven | 3.9+ | Build tool |

### Optional (for production persistence)

| Technology | Purpose |
|------------|---------|
| Spring Data JPA | Database access |
| PostgreSQL (Neon) | Cloud database |

---

## 17. Future Improvements

- [ ] **Database Migration**: Switch from in-memory stores to Neon PostgreSQL
- [ ] **Rate Limiting**: Implement token bucket algorithm for API rate limiting
- [ ] **Caching**: Redis-based caching for frequent status checks
- [ ] **Metrics**: Prometheus metrics for transaction throughput, latency, error rates
- [ ] **Webhook Signatures**: Validate webhook payloads using HMAC signatures
- [ ] **Kubernetes**: Helm charts for Kubernetes deployment
- [ ] **CI/CD**: GitHub Actions pipeline for automated testing and deployment
- [ ] **Idempotency**: Improve idempotency with idempotency keys
- [ ] **Audit Trail**: Complete audit logging of all state changes
- [ ] **Circuit Breaker**: Resilience4j circuit breaker for PawaPay API calls