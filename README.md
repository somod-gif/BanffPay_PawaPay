# BanffPay PawaPay Integration

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![Maven](https://img.shields.io/badge/Maven-3.9-orange)
![License](https://img.shields.io/badge/License-Proprietary-red)

**A production-grade REST API for mobile money deposit collection and payout disbursement via the PawaPay API v2.**

---

## 1. Project Overview

BanffPay PawaPay Integration serves as a middleware layer between BanffPay's internal systems and PawaPay's payment infrastructure. It enables:

- **Deposit Collection** — Collect mobile money from customers via MMO wallets (M-Pesa, MTN MoMo, Airtel Money, etc.)
- **Payout Disbursement** — Send funds to customers' mobile money wallets
- **Transaction Status Tracking** — Real-time status with automatic synchronization from PawaPay
- **Webhook Callbacks** — Receive asynchronous transaction status updates from PawaPay
- **Multi-Country Support** — Kenya, Uganda, Ghana, Tanzania (easily extensible)
- **Correlation ID Tracing** — End-to-end request tracking across all systems

### Supported Countries & Providers

| Country | Code | Currency | Providers |
|---------|------|----------|-----------|
| Kenya | KE | KES | Safaricom, Airtel Kenya |
| Uganda | UG | UGX | MTN Uganda, Airtel Uganda |
| Ghana | GH | GHS | MTN Ghana, Vodafone Ghana, AirtelTigo Ghana |
| Tanzania | TZ | TZS | Vodacom Tanzania, Tigo Tanzania, Airtel Tanzania, Halotel Tanzania |

---

## 2. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CLIENT APPLICATIONS                            │
│              (Web App, Mobile App, Internal Systems)                    │
└───────────────────────┬─────────────────────────────────────────┘
                        │ HTTP/HTTPS (REST JSON)
                        │ X-Correlation-Id, X-Idempotency-Key
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
│  │  │  (RestClient → POST /deposits, POST /payouts,             │   │  │
│  │  │   GET /deposits/{id}, GET /payouts/{id})                  │   │  │
│  │  └────────────────────────────────────────────────────────────┘   │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    DATA LAYER                                    │   │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐  │   │
│  │  │ Transaction      │  │ CountryConfig    │  │ Transaction   │  │   │
│  │  │ Entity (JPA)     │  │ (multi-country)  │  │ Repository    │  │   │
│  │  └──────────────────┘  └──────────────────┘  └───────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    INFRASTRUCTURE LAYER                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │   │
│  │  │ Correlation  │  │ Global       │  │ OpenAPI / Swagger    │   │   │
│  │  │ ID Filter    │  │ Exception    │  │ Documentation        │   │   │
│  │  │              │  │ Handler      │  │                      │   │   │
│  │  └──────────────┘  └──────────────┘  └──────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ HTTPS (REST JSON)
                                │ Bearer Token Auth
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
   │   amount, currency,      │                          │                         │
   │   country, provider,     │                          │                         │
   │   phoneNumber,           │                          │                         │
   │   customerName           │                          │                         │
   │  }                       │                          │                         │
   │─────────────────────────>│                          │                         │
   │                          │                          │                         │
   │                          │  Check duplicate         │                         │
   │                          │  Validate country/       │                         │
   │                          │  provider                │                         │
   │                          │                          │                         │
   │                          │  Create Transaction      │                         │
   │                          │  (status=PENDING)        │                         │
   │                          │                          │                         │
   │                          │  POST /deposits          │                         │
   │                          │  { depositId, payer,     │                         │
   │                          │    amount, currency }    │                         │
   │                          │─────────────────────────>│                         │
   │                          │                          │                         │
   │                          │                          │  Forward to operator    │
   │                          │                          │────────────────────────>│
   │                          │                          │                         │
   │                          │  Deposit Response        │                         │
   │                          │  { depositId, status     │                         │
   │                          │    = "PROCESSING" }      │                         │
   │                          │<─────────────────────────│                         │
   │                          │                          │                         │
   │                          │  Update Transaction      │                         │
   │                          │  (status=PROCESSING,     │                         │
   │                          │   pawapayId=depositId)   │                         │
   │                          │                          │                         │
   │  200 OK                  │                          │                         │
   │  { transactionId,        │                          │                         │
   │    status="PROCESSING" } │                          │                         │
   │<─────────────────────────│                          │                         │
   │                          │                          │                         │
   │                          │  ─── ASYNC ───           │                         │
   │                          │                          │                         │
   │                          │  <───────── Webhook Callback ─────────────         │
   │                          │  { depositId,                                     │
   │                          │    status="COMPLETED" }                           │
   │                          │                          │                         │
   │                          │  Update Transaction      │                         │
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
   │   amount, currency,      │                          │                         │
   │   country, provider,     │                          │                         │
   │   phoneNumber,           │                          │                         │
   │   customerName           │                          │                         │
   │  }                       │                          │                         │
   │─────────────────────────>│                          │                         │
   │                          │  Validate + Persist      │                         │
   │                          │                          │                         │
   │                          │  POST /payouts           │                         │
   │                          │─────────────────────────>│                         │
   │                          │                          │────────────────────────>│
   │                          │                          │                         │
   │                          │  Payout Response         │                         │
   │                          │<─────────────────────────│                         │
   │                          │                          │                         │
   │  Response                │                          │                         │
   │<─────────────────────────│                          │                         │
```

---

## 5. Deposit Status Flow

```
 Client                    BanffPay                    PawaPay
   │                          │                          │
   │  GET /api/deposits/       │                          │
   │  {transactionId}          │                          │
   │─────────────────────────>│                          │
   │                          │  Lookup local transaction │
   │                          │                          │
   │                          │  GET /deposits/{id}      │
   │                          │─────────────────────────>│
   │                          │  Status Response         │
   │                          │<─────────────────────────│
   │                          │                          │
   │                          │  Sync status if changed  │
   │                          │                          │
   │  Status Response         │                          │
   │<─────────────────────────│                          │
```

---

## 6. Payout Status Flow

Same pattern as Deposit Status Flow but using `GET /api/payouts/{transactionId}` and `GET /payouts/{payoutId}`.

---

## 7. Webhook Flow

```
 PawaPay                  BanffPay Webhook              Database
   │                          │                            │
   │  POST /api/webhooks/     │                            │
   │  pawapay                 │                            │
   │  { depositId/payoutId,   │                            │
   │    status, amount,       │                            │
   │    currency, ... }       │                            │
   │─────────────────────────>│                            │
   │                          │  Validate payload          │
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

### Deposit Webhook Handling

When PawaPay sends a webhook with `depositId`:
1. The `WebhookController` receives the POST request at `/api/webhooks/pawapay`
2. The payload is validated (must have `depositId` and `status`)
3. `WebhookService` looks up the transaction by `pawapayId` in the database
4. If found, the transaction status is updated (e.g., from PROCESSING to COMPLETED)
5. Returns HTTP 200 to acknowledge receipt

### Payout Webhook Handling

Same process, but PawaPay sends `payoutId` instead of `depositId`. The service determines the event type using `isDepositEvent()`.

---

## 8. Configuration Setup

### Application Configuration (`application.yaml`)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:banffpaydb
    driver-class-name: org.h2.Driver

pawapay:
  base-url: https://api.sandbox.pawapay.io/v2
  api-key: ${PAWAPAY_API_KEY}
  connect-timeout-ms: 5000
  read-timeout-ms: 10000
  max-retries: 3

server:
  port: 8080
```

### Multi-Country Configuration

The `CountryConfig` class provides a clean, extensible mapping of supported countries. To add a new country:

```java
// In CountryConfig.init()
Country nigeria = Country.builder()
    .code("NG")
    .name("Nigeria")
    .currency("NGN")
    .providers(List.of(
        Provider.builder().code("MTN_NG").name("MTN Nigeria").build(),
        Provider.builder().code("AIRTEL_NG").name("Airtel Nigeria").build(),
        Provider.builder().code("GLO_NG").name("Globacom Nigeria").build()
    ))
    .build();
countries.put("NG", nigeria);
```

---

## 9. Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `PAWAPAY_API_KEY` | Yes | - | PawaPay API Bearer token |
| `SERVER_PORT` | No | 8080 | Application port |
| `SPRING_PROFILES_ACTIVE` | No | - | Spring profile (dev, prod) |

---

## 10. Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- Ngrok (for webhook testing)

### Steps

```bash
# 1. Clone the repository
git clone <repository-url>
cd banffypay-pawapay

# 2. Set PawaPay API key (or use default sandbox key)
export PAWAPAY_API_KEY=your-api-key-here

# 3. Build the project
./mvnw clean package -DskipTests

# 4. Run the application
./mvnw spring-boot:run

# 5. Access the API
curl http://localhost:8080/api/deposits
curl http://localhost:8080/swagger-ui.html
curl http://localhost:8080/actuator/health
```

### Quick Test with cURL

```bash
# Initiate a deposit (Kenya - Safaricom)
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "merchantTransactionId": "INV-20240609-001",
    "phoneNumber": "254712345678",
    "country": "KE",
    "amount": "500.00",
    "currency": "KES",
    "provider": "SAFARICOM"
  }'

# Initiate a payout (Uganda - MTN)
curl -X POST http://localhost:8080/api/payouts \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Doe",
    "merchantTransactionId": "PO-20240609-001",
    "phoneNumber": "256712345678",
    "country": "UG",
    "amount": "200.00",
    "currency": "UGX",
    "provider": "MTN_UG"
  }'

# Check transaction status
curl http://localhost:8080/api/deposits/{transactionId}
curl http://localhost:8080/api/payouts/{transactionId}
```

---

## 11. Testing

### Running Tests

```bash
# Run all tests
./mvnw clean test

# Run specific test class
./mvnw test -Dtest=DepositServiceTest

# Run with coverage report
./mvnw clean verify
```

### Test Coverage

| Test Class | Coverage |
|------------|----------|
| `DepositServiceTest` | Deposit creation, duplicate detection, country validation, status sync |
| `PayoutServiceTest` | Payout creation, duplicate detection, provider validation, status sync |
| `WebhookServiceTest` | Webhook processing, deposit/payout callbacks, validation, fallback lookup |

### Test Scenarios Covered

- **Success**: Deposit/payout initiated, transaction persisted, PawaPay called successfully
- **Duplicate**: Merchant transaction ID uniqueness enforced (409 Conflict)
- **Validation**: Invalid country, unsupported provider (400 Bad Request)
- **Not Found**: Unknown transaction ID (404)
- **Type Mismatch**: Deposit ID on payout endpoint (400 Bad Request)
- **Status Sync**: Automatic status synchronization from PawaPay
- **Webhook Events**: Deposit completed, payout failed, deposit cancelled
- **Webhook Fallback**: Lookup by clientReferenceId when pawapayId not found
- **Webhook Validation**: Missing transaction ID or status (400 Bad Request)

---

## 12. Fintech Engineering Review

### Code Quality & Architecture

✅ **Clean Architecture**: Separation into config, controller, service, client, dto, model, webhook, exception, util, repository
✅ **SOLID Principles**: Single Responsibility (each service/controller focused), Dependency Injection (constructor injection), Interface Segregation
✅ **SOLID - Open/Closed**: CountryConfig allows adding new countries without modifying business logic
✅ **DRY**: Reusable ApiResponse wrapper, PawapayMapper for DTO conversion
✅ **Structured Logging**: SLF4J with correlation IDs, consistent log format for log aggregation

### Security

✅ **API Key Protection**: `api-key` loaded from environment variable `${PAWAPAY_API_KEY}` (not hardcoded)
✅ **Bearer Token Auth**: PawaPay API calls authenticated via Bearer token
✅ **Input Validation**: Jakarta Validation on all request DTOs (NotBlank, Pattern, Size, DecimalMin)
✅ **PII Protection**: Metadata marked as `isPII: true` per PawaPay spec
✅ **Secure Defaults**: Jackson serialization excludes null values

### Performance & Scalability

✅ **Connection Pooling**: RestClient uses default connection pool
✅ **Read Timeouts**: Configurable timeouts (connect=5s, read=10s)
✅ **Stateless Design**: No session state — horizontally scalable
✅ **Asynchronous Webhooks**: Returns 200 immediately, processes asynchronously
✅ **Transactional Boundaries**: `@Transactional` on write operations, `readOnly=true` on reads

### Missing Validations & Edge Cases

✅ **Amount Validation**: DecimalMin, Digits constraint on amount field
✅ **Phone Validation**: Pattern constraint (digits only), Size (7-15 chars)
✅ **Currency Validation**: 3-letter ISO code pattern
✅ **Country Validation**: 2-letter ISO code pattern
✅ **Duplicate Detection**: merchantTransactionId uniqueness check
✅ **Transaction Type Guard**: Deposit ID cannot query payout endpoint and vice versa
✅ **Null Safety**: PawapayClient null-checks responses before accessing fields

### Code Smells Addressed

✅ **Hardcoded Values Removed**: Country/provider mappings via CountryConfig
✅ **Exception Handling**: Global exception handler with error codes
✅ **Logging Consistency**: Structured key=value log format for log aggregation (ELK/Datadog)
✅ **Magic Strings Removed**: Error codes centralized in `ErrorCode` class

---

## 13. Ngrok Webhook Testing

### Step 1: Install Ngrok

Download Ngrok from [ngrok.com/download](https://ngrok.com/download) and add it to your PATH.

### Step 2: Start the Application

```bash
export PAWAPAY_API_KEY=your-api-key
./mvnw spring-boot:run
```

### Step 3: Start Ngrok

```bash
ngrok http 8080
```

You will see output similar to:
```
Forwarding  https://tasty-porcupine-cabdriver.ngrok-free.dev -> http://localhost:8080
```

### Step 4: Configure PawaPay Callback URL

In your PawaPay dashboard (or API configuration), set the webhook callback URL to:

```
https://tasty-porcupine-cabdriver.ngrok-free.dev/api/webhooks/pawapay
```

### Step 5: Simulate a Webhook Callback

```bash
curl -X POST https://tasty-porcupine-cabdriver.ngrok-free.dev/api/webhooks/pawapay \
  -H "Content-Type: application/json" \
  -d '{
    "depositId": "pawapay-deposit-123",
    "status": "COMPLETED",
    "amount": "500.00",
    "currency": "KES",
    "country": "KE"
  }'
```

### Step 6: Verify Callback Delivery

Check the application logs:
```
webhook_callback_received correlationId=... pawapayId=pawapay-deposit-123 status=COMPLETED
webhook_callback_processed correlationId=... pawapayId=pawapay-deposit-123 status=COMPLETED
webhook_status_updated correlationId=... transactionId=... pawapayId=pawapay-deposit-123 oldStatus=PROCESSING newStatus=COMPLETED
```

### Sample Callback Payloads

**Deposit Completed:**
```json
{
  "depositId": "pawapay-deposit-123",
  "status": "COMPLETED",
  "amount": "500.00",
  "currency": "KES",
  "country": "KE",
  "created": "2026-06-09T10:00:00Z",
  "clientReferenceId": "INV-001"
}
```

**Payout Failed:**
```json
{
  "payoutId": "pawapay-payout-456",
  "status": "FAILED",
  "amount": "200.00",
  "currency": "UGX",
  "country": "UG",
  "errors": [
    {
      "errorCode": "INSUFFICIENT_BALANCE",
      "errorMessage": "Insufficient balance for payout"
    }
  ]
}
```

### Expected Logs

| Event | Log Message |
|-------|-------------|
| Callback received | `webhook_callback_received correlationId=... pawapayId=... status=...` |
| Payload validated | `webhook_event_validated pawapayId=... status=...` |
| Transaction not found | `webhook_transaction_not_found correlationId=... pawapayId=...` |
| Status updated | `webhook_status_updated correlationId=... transactionId=... oldStatus=... newStatus=...` |
| Status unchanged | `webhook_status_unchanged correlationId=... transactionId=... status=...` |

### Troubleshooting

**404 Errors:**
- Verify the Ngrok URL is correct: `https://{ngrok-id}.ngrok-free.dev/api/webhooks/pawapay`
- Check the application is running on port 8080
- Verify Ngrok is forwarding correctly: `ngrok http 8080`

**401 Errors:**
- This application does not require auth for webhooks (PawaPay uses IP whitelisting)
- Check if your Ngrok URL is properly configured in PawaPay dashboard

**Timeout Errors:**
- Ensure the application is not under heavy load
- Check database connectivity
- Verify PawaPay API is reachable from your network

**Invalid Payloads:**
- The webhook endpoint returns 400 Bad Request for invalid payloads
- Logged as `webhook_validation_failed pawapayId=... status is null or empty`
- Ensure both `depositId`/`payoutId` and `status` fields are present

### End-to-End Flow Verification

```
1. Client sends POST /api/deposits
2. Spring Boot processes → calls PawaPay API
3. PawaPay processes → sends webhook callback to Ngrok URL
4. Ngrok forwards to localhost:8080/api/webhooks/pawapay
5. Spring Boot WebhookController receives the callback
6. WebhookService updates transaction status in database
7. Client can verify by calling GET /api/deposits/{transactionId}
```

---

## 14. API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/deposits` | Initiate a mobile money deposit |
| `GET` | `/api/deposits/{transactionId}` | Get deposit status |
| `POST` | `/api/payouts` | Initiate a mobile money payout |
| `GET` | `/api/payouts/{transactionId}` | Get payout status |
| `POST` | `/api/webhooks/pawapay` | Receive PawaPay webhook callbacks |
| `GET` | `/swagger-ui.html` | Swagger UI (API documentation) |
| `GET` | `/v3/api-docs` | OpenAPI specification |
| `GET` | `/actuator/health` | Health check endpoint |
| `GET` | `/h2-console` | H2 database console |

---

## 15. Transaction States

| Status | Description |
|--------|-------------|
| `PENDING` | Initial state when transaction record is first created |
| `PROCESSING` | After successful submission to PawaPay |
| `COMPLETED` | Transaction completed successfully |
| `FAILED` | Transaction failed |
| `CANCELLED` | Transaction was cancelled |

### Transaction Types

| Type | Description |
|------|-------------|
| `DEPOSIT` | Mobile money collection from customer |
| `PAYOUT` | Mobile money disbursement to customer |

---

## 16. Future Improvements

- [ ] **Database Migration**: Replace H2 with PostgreSQL for production
- [ ] **Rate Limiting**: Implement token bucket algorithm for API rate limiting
- [ ] **Caching**: Redis-based caching for frequent status checks
- [ ] **Metrics**: Prometheus metrics for transaction throughput, latency, error rates
- [ ] **Retry Logic**: Exponential backoff for failed PawaPay API calls
- [ ] **Webhook Signatures**: Validate webhook payloads using HMAC signatures
- [ ] **Kubernetes**: Helm charts for Kubernetes deployment
- [ ] **CI/CD**: GitHub Actions pipeline for automated testing and deployment
- [ ] **Scheduled Reconciliation**: Batch job to reconcile transactions with PawaPay
- [ ] **Idempotency**: Improve idempotency with idempotency keys
- [ ] **Audit Trail**: Complete audit logging of all state changes
- [ ] **Circuit Breaker**: Resilience4j circuit breaker for PawaPay API calls

---

## 17. Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.3.0 | Framework |
| Spring Data JPA | 3.3.0 | Database access |
| H2 Database | - | Embedded database (dev) |
| Lombok | - | Boilerplate reduction |
| SpringDoc OpenAPI | 2.5.0 | API documentation |
| SLF4J + Logback | - | Logging |
| JUnit 5 + Mockito | - | Testing |
| Maven | 3.9+ | Build tool |

---

## Project Structure

```
com.banffpay.pawapay/
├── BanffpayPawapayApplication.java    # Spring Boot entry point
├── config/
│   ├── CountryConfig.java             # Multi-country provider mappings
│   ├── OpenApiConfig.java             # Swagger/OpenAPI configuration
│   ├── PawapayProperties.java         # PawaPay configuration properties
│   └── RestClientConfig.java          # RestClient bean configuration
├── controller/
│   ├── DepositController.java         # Deposit REST endpoints
│   └── PayoutController.java          # Payout REST endpoints
├── client/
│   ├── PawapayClient.java             # PawaPay API client
│   └── dto/                           # PawaPay API DTOs
│       ├── PawapayDepositRequest.java
│       ├── PawapayDepositResponse.java
│       ├── PawapayPayoutRequest.java
│       ├── PawapayPayoutResponse.java
│       ├── PawapayPayer.java
│       ├── PawapayRecipient.java
│       ├── PawapayAccountDetails.java
│       └── PawapayError.java
├── dto/
│   ├── request/
│   │   ├── DepositRequest.java        # Validated deposit request
│   │   └── PayoutRequest.java         # Validated payout request
│   └── response/
│       ├── ApiResponse.java           # Standardized response wrapper
│       ├── DepositResponse.java
│       └── PayoutResponse.java
├── model/
│   ├── Country.java                   # Country domain model
│   ├── Provider.java                  # Provider domain model
│   ├── entity/
│   │   └── Transaction.java           # JPA transaction entity
│   └── enums/
│       ├── TransactionStatus.java     # PENDING, PROCESSING, COMPLETED, etc.
│       └── TransactionType.java       # DEPOSIT, PAYOUT
├── service/
│   ├── DepositService.java            # Deposit business logic
│   ├── PayoutService.java             # Payout business logic
│   └── mapper/
│       └── PawapayMapper.java         # DTO mapper
├── webhook/
│   ├── WebhookController.java         # Webhook REST endpoint
│   ├── WebhookService.java            # Webhook processing logic
│   └── dto/
│       └── WebhookEvent.java          # Webhook callback payload
├── exception/
│   ├── ApiException.java              # Custom exception with HTTP status
│   ├── ErrorCode.java                 # Standardized error codes
│   ├── GlobalExceptionHandler.java    # @RestControllerAdvice handler
│   └── ResourceNotFoundException.java # 404 exception
├── repository/
│   └── TransactionRepository.java     # JPA repository
└── util/
    └── CorrelationIdFilter.java       # Request tracing filter