# BanffPay PawaPay Integration — Complete Documentation

## 📋 Project Overview

**BanffPay PawaPay Integration** is a Spring Boot REST API middleware that connects BanffPay's internal systems to **PawaPay API v2** for mobile money payments. It enables deposit collection and payout disbursement across Zambia and Uganda.

- **Java 21** + **Spring Boot 3.3**
- **API v2 endpoints**: `https://api.sandbox.pawapay.io/v2`
- **In-memory store** (ConcurrentHashMap) — no database required
- **Swagger UI**: `/swagger-ui.html`
- **OpenAPI spec**: `/v3/api-docs`

---

## 🏗 Architecture

```
┌──────────────────────┐
│   Postman / Client   │
│  (merchantTransactionId, customerName, phoneNumber, country, currency, amount, provider)
└─────────┬────────────┘
          │
          ▼
┌──────────────────────────────────────────────┐
│         CONTROLLER LAYER                      │
│  DepositController  │  PayoutController       │
│  POST /api/deposits │  POST /api/payouts      │
│  GET  /{txId}       │  GET  /{txId}           │
└─────────┬──────────────────┬─────────────────┘
          │                  │
          ▼                  ▼
┌──────────────────────────────────────────────┐
│         SERVICE LAYER                         │
│  DepositService  │  PayoutService             │
│  (validation +   │  (validation +             │
│   transformation │   transformation           │
│   to PawaPay     │   to PawaPay format)       │
│   format)        │                            │
└─────────┬──────────────────┬─────────────────┘
          │                  │
          ▼                  ▼
┌──────────────────────────────────────────────┐
│         CLIENT LAYER                          │
│         PawapayClient                         │
│  POST /v2/deposits │  POST /v2/payouts        │
│  GET /v2/deposits/{}│  GET /v2/payouts/{}     │
│         (Bearer Auth + Idempotency-Key)       │
└─────────┬──────────────────┬─────────────────┘
          │                  │
          ▼                  ▼
┌──────────────────────────────────────────────┐
│         PAWAPAY SANDBOX API                   │
│   https://api.sandbox.pawapay.io/v2          │
└──────────────────────────────────────────────┘

          ┌─────────────────────────┐
          │   WEBHOOK FLOW          │
          │                         │
          │  PawaPay ──────────►    │
          │  POST /api/webhooks/    │
          │       pawapay           │
          │  { pawapayId, type,    │
          │    status }             │
          └─────────────────────────┘
```

---

## 🌍 Supported Countries

| Country | Country Code | Currency | Provider | Phone Example |
|---------|-------------|----------|----------|---------------|
| Zambia | `ZM` or `ZMB` | `ZMW` | `MTN_MOMO_ZMB` | `260763456789` |
| Uganda | `UG` | `UGX` | `MTN_MOMO_UGA` | `256700123456` |

**Note:** Both `ZM` and `ZMB` are accepted as Zambia country codes.

---

## 🔌 API Endpoints

| Method | Endpoint | Description | Status Code |
|--------|----------|-------------|-------------|
| `POST` | `/api/deposits` | Initiate deposit | `200` |
| `GET` | `/api/deposits/{transactionId}` | Get deposit status | `200` |
| `POST` | `/api/payouts` | Initiate payout | `200` |
| `GET` | `/api/payouts/{transactionId}` | Get payout status | `200` |
| `POST` | `/api/webhooks/pawapay` | Receive PawaPay webhook | `200` |
| `GET` | `/swagger-ui.html` | Swagger UI | `200` |
| `GET` | `/v3/api-docs` | OpenAPI JSON spec | `200` |

---

## 📦 Deposit Flow

### 1. Client sends POST `/api/deposits`

**Request Body:**
```json
{
  "merchantTransactionId": "INV-260763456789",
  "customerName": "Eniola",
  "phoneNumber": "260763456789",
  "country": "ZMB",
  "currency": "ZMW",
  "amount": 20,
  "provider": "MTN_MOMO_ZMB"
}
```

### 2. Backend transforms to PawaPay v2 format
```json
{
  "depositId": "uuid-generated",
  "payer": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "260763456789",
      "provider": "MTN_MOMO_ZMB"
    }
  },
  "amount": "20",
  "currency": "ZMW",
  "clientReferenceId": "INV-260763456789",
  "customerMessage": "Deposit INV-260763456789"
}
```

### 3. PawaPay response
```json
{
  "depositId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "status": "ACCEPTED",
  "created": "2026-06-10T10:00:00Z"
}
```

### 4. Response to client
```json
{
  "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
  "merchantTransactionId": "INV-260763456789",
  "customerName": "Eniola",
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "ACCEPTED",
  "amount": 20,
  "currency": "ZMW",
  "createdAt": "2026-06-10T10:19:43.697Z"
}
```

---

## 📦 Payout Flow

### 1. Client sends POST `/api/payouts`

**Request Body:**
```json
{
  "customerName": "Jane Doe",
  "merchantTransactionId": "INV-673476476",
  "phoneNumber": "260763456789",
  "country": "ZM",
  "amount": "15",
  "currency": "ZMW",
  "provider": "MTN_MOMO_ZMB"
}
```

### 2. Backend transforms to PawaPay v2 format
```json
{
  "payoutId": "uuid-generated",
  "recipient": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "260763456789",
      "provider": "MTN_MOMO_ZMB"
    }
  },
  "amount": "15",
  "currency": "ZMW",
  "clientReferenceId": "INV-673476476",
  "customerMessage": "Payout INV-673476476"
}
```

### 3. Response to client
```json
{
  "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
  "merchantTransactionId": "INV-673476476",
  "customerName": "Jane Doe",
  "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
  "type": "PAYOUT",
  "status": "ACCEPTED",
  "amount": 15,
  "currency": "ZMW",
  "createdAt": "2026-06-10T10:19:43.697Z"
}
```

---

## 🔄 Status Check

### GET `/api/deposits/{transactionId}`

Response:
```json
{
  "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
  "merchantTransactionId": "INV-260763456789",
  "customerName": "Eniola",
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "ACCEPTED",
  "amount": 20,
  "currency": "ZMW",
  "createdAt": "2026-06-10T10:19:43.697Z"
}
```

The backend also calls PawaPay's status endpoint to sync the latest status before returning.

---

## 🔔 Webhook

### PawaPay sends POST to `/api/webhooks/pawapay`

**Request Body:**
```json
{
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
```

**Response:**
```json
{
  "message": "Webhook processed successfully"
}
```

**Webhook Logic:**
1. Validates status is one of: `ACCEPTED`, `PROCESSING`, `COMPLETED`, `FAILED`, `REJECTED`, `CANCELLED`
2. Looks up transaction by `pawapayId` in in-memory store
3. Updates the transaction status
4. Returns 200 OK

---

## 📊 API Response Codes

| Code | Description |
|------|-------------|
| **200** | ✅ Success |
| **202** | ⏳ Accepted / Processing |
| **400** | ❌ Bad Request — Validation error |
| **401** | 🔒 Not Authenticated |
| **403** | 🚫 Not Allowed |
| **404** | 🔍 Not Found |
| **409** | 🔄 Duplicate / Conflict |
| **422** | 📋 Validation Rule Failed |
| **500** | 💥 Internal Server Error — Backend crashed |
| **502** | 🌐 Bad Gateway — External service failed |
| **503** | 🔧 Service Down |
| **504** | ⏰ Gateway Timeout |

---

## 🔧 Configuration

### `application.yaml`
```yaml
pawapay:
  base-url: https://api.sandbox.pawapay.io
  api-key: ${PAWAPAY_API_KEY:your-api-key-here}
  deposit:
    endpoint: /v2/deposits
  payout:
    endpoint: /v2/payouts
```

### API Key Setup
The API key is configured in `application.yaml`:
```yaml
pawapay:
  api-key: ${PAWAPAY_API_KEY:eyJraWQiOiIxIi...your-key-here...}
```

The `${PAWAPAY_API_KEY:default-value}` syntax means:
- If environment variable `PAWAPAY_API_KEY` is set → uses that
- Otherwise → falls back to the default value

---

## 🚀 How to Run

### Prerequisites
- Java 21+
- Maven 3.9+
- Ngrok (for webhook testing)

### Steps
```bash
# 1. Start the application
mvn spring-boot:run

# 2. Open Swagger UI
# http://localhost:8080/swagger-ui.html
```

### Test with curl

**Deposit — Zambia (ZMW):**
```bash
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "merchantTransactionId": "INV-260763456789",
    "customerName": "Eniola",
    "phoneNumber": "260763456789",
    "country": "ZMB",
    "currency": "ZMW",
    "amount": 20,
    "provider": "MTN_MOMO_ZMB"
  }'
```

**Deposit — Uganda (UGX):**
```bash
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "merchantTransactionId": "INV-256700123456",
    "customerName": "Peter",
    "phoneNumber": "256700123456",
    "country": "UG",
    "currency": "UGX",
    "amount": 50000,
    "provider": "MTN_MOMO_UGA"
  }'
```

**Payout — Zambia (ZMW):**
```bash
curl -X POST http://localhost:8080/api/payouts \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Doe",
    "merchantTransactionId": "INV-673476476",
    "phoneNumber": "260763456789",
    "country": "ZM",
    "amount": "15",
    "currency": "ZMW",
    "provider": "MTN_MOMO_ZMB"
  }'
```

---

## 🌐 Ngrok Webhook Testing

```bash
# Terminal 1: Start the app
mvn spring-boot:run

# Terminal 2: Start ngrok
ngrok http 8080
```



**Webhook URL to configure in PawaPay:**
```
https://tasty-porcupine-cabdriver.ngrok-free.dev/api/webhooks/pawapay
```

**Test the webhook:**
```bash
curl -X POST https://tasty-porcupine-cabdriver.ngrok-free.dev/api/webhooks/pawapay \
  -H "Content-Type: application/json" \
  -d '{
    "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
    "type": "DEPOSIT",
    "status": "COMPLETED"
  }'
```

---

## 🧪 Postman Testing Flow

### Test 1: Initiate Deposit
```
POST /api/deposits
→ Expect 200, status: "ACCEPTED"
```

### Test 2: Get Deposit Status
```
GET /api/deposits/{transactionId}
→ Expect 200, status: "ACCEPTED"
```

### Test 3: Simulate Webhook (Deposit Completed)
```
POST /api/webhooks/pawapay
{
  "pawapayId": "returned-pawapay-id",
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
→ Expect 200
```

### Test 4: Verify Status Updated
```
GET /api/deposits/{transactionId}
→ Expect 200, status: "COMPLETED"
```

### Test 5: Initiate Payout
```
POST /api/payouts
→ Expect 200, status: "ACCEPTED"
```

### Test 6: Get Payout Status
```
GET /api/payouts/{transactionId}
→ Expect 200, status: "ACCEPTED"
```

### Test 7: Simulate Webhook (Payout Completed)
```
POST /api/webhooks/pawapay
{
  "pawapayId": "returned-pawapay-id",
  "type": "PAYOUT",
  "status": "COMPLETED"
}
→ Expect 200
```

### Test 8: Verify Payout Status Updated
```
GET /api/payouts/{transactionId}
→ Expect 200, status: "COMPLETED"
```

---

## 📁 Project Structure

```
src/main/java/com/banffpay/pawapay/
├── BanffpayPawapayApplication.java      # Spring Boot entry point
├── config/
│   ├── CorsConfig.java                  # CORS filter (all origins allowed)
│   └── OpenApiConfig.java               # Swagger/OpenAPI config
├── controller/
│   ├── DepositController.java           # Deposit REST endpoints
│   ├── PayoutController.java            # Payout REST endpoints
│   └── WebhookController.java           # Webhook REST endpoint
├── service/
│   ├── DepositService.java              # Deposit business logic
│   ├── PayoutService.java               # Payout business logic
│   └── WebhookService.java              # Webhook processing logic
├── client/
│   ├── PawapayClient.java               # PawaPay API HTTP client
│   └── PawapayProperties.java           # Configuration properties
├── dto/
│   ├── DepositRequest.java              # Deposit request DTO
│   ├── PayoutRequest.java               # Payout request DTO
│   ├── TransactionResponse.java         # Standard response DTO
│   ├── WebhookRequest.java              # Webhook request DTO
│   ├── DepositResponse.java             # (Unused, kept for reference)
│   └── PayoutResponse.java              # (Unused, kept for reference)
├── model/
│   └── Transaction.java                 # Transaction domain model
├── store/
│   └── TransactionStore.java            # In-memory ConcurrentHashMap store
└── exception/
    └── GlobalExceptionHandler.java      # Global error handler
```

---

## 🔑 Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **String amount** in requests | Accept both `20` (number) and `"15"` (string) from Postman |
| **String amount** in PawaPay API | PawaPay v2 requires amount as string |
| **BigDecimal amount** in response | Standard Java decimal type for monetary values |
| **ZM/ZMB both accepted** | User may send either 2-letter or 3-letter country code |
| **In-memory store** | No external database dependency; simple ConcurrentHashMap |
| **UUID generation** | Backend generates depositId/payoutId internally |
| **200 for POST** | Matches user requirement for success responses |
| **Idempotency-Key header** | Prevents duplicate PawaPay transactions |
| **No depositId/payoutId in request** | Backend auto-generates these; client only sends merchantTransactionId |

---

## ✅ Files Modified (Complete List)

| File | Change |
|------|--------|
| `DepositRequest.java` | Flattened: `merchantTransactionId`, `customerName`, `phoneNumber`, `country`, `currency`, `amount` (String), `provider` |
| `PayoutRequest.java` | Flattened: `merchantTransactionId`, `customerName`, `phoneNumber`, `country`, `currency`, `amount` (String), `provider` |
| `TransactionResponse.java` | Added `@Schema` annotations, `customerName` field |
| `WebhookRequest.java` | Changed `paymentId` → `pawapayId` |
| `Transaction.java` | Added `customerName`, `provider` fields |
| `TransactionStore.java` | Added `findByPawapayId()` with bidirectional index |
| `DepositService.java` | Full rewrite: reads flat fields, validates country+currency+provider, calls PawapayClient, returns TransactionResponse |
| `PayoutService.java` | Full rewrite: same pattern as deposit |
| `WebhookService.java` | Updated to use `pawapayId`, expanded valid statuses |
| `DepositController.java` | Returns `TransactionResponse`, 200 for success, comprehensive Swagger @ApiResponses |
| `PayoutController.java` | Returns `TransactionResponse`, 200 for success, comprehensive Swagger @ApiResponses |
| `PawapayClient.java` | Updated to use v2 endpoint paths |
| `PawapayProperties.java` | Default endpoints changed to `/v2/deposits` and `/v2/payouts` |
| `application.yaml` | base-url → `https://api.sandbox.pawapay.io`, endpoints → `/v2/deposits`, `/v2/payouts` |
| `OpenApiConfig.java` | Added ngrok server URL |
| `CorsConfig.java` | Added ngrok origin |
| `GlobalExceptionHandler.java` | Improved error response format |
| All 3 test files | Complete rewrite to match new DTOs and service logic |

---

## 📈 Test Results

```
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS ✅
```

| Test Class | Tests | Coverage |
|------------|-------|----------|
| DepositServiceTest | 8 | Zambia success, Uganda success, ZM shortcode, invalid country, invalid currency, wrong currency, invalid provider, status check |
| PayoutServiceTest | 7 | Zambia success, Uganda success, invalid country, invalid currency, wrong currency, invalid provider, status check |
| WebhookServiceTest | 4 | Completed, Failed, invalid status, transaction not found |
| **Total** | **19** | **All passing** |

---

## 🐞 Troubleshooting

| Problem | Solution |
|---------|----------|
| `"Unsupported country"` | Use `ZM`, `ZMB`, or `UG` |
| `"Invalid currency"` | Use `ZMW` for Zambia, `UGX` for Uganda |
| `"Invalid provider"` | Use `MTN_MOMO_ZMB` for ZMW, `MTN_MOMO_UGA` for UGX |
| `"Transaction not found"` | Use the `transactionId` from the create response, not `depositId`/`payoutId` |
| Port 8080 in use | Kill existing process or change port in `application.yaml` |
| Webhook failing | Ensure ngrok is running and URL is correct |
| PawaPay returning errors | Check API key and sandbox URL |