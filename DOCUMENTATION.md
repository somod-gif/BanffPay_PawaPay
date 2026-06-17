# BanffPay PawaPay Integration — Complete Documentation

## 📋 Project Overview

**BanffPay PawaPay Integration** is a Spring Boot REST API middleware that connects BanffPay's internal systems to **PawaPay API v2** for mobile money payments across multiple African countries.

- **Java 21** + **Spring Boot 3.3**
- **API v2 endpoints**: `https://api.sandbox.pawapay.io/v2`
- **In-memory store** (ConcurrentHashMap) — no database required
- **Swagger UI**: `/swagger-ui.html`
- **OpenAPI spec**: `/v3/api-docs`
- **Supported countries**: Uganda, Zambia, Rwanda, Tanzania, Kenya, Nigeria, South Africa + Cameroon, Benin

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
┌──────────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                                   │
│  ┌────────────────────────────────────────────────────────┐         │
│  │              CountryValidationService                  │         │
│  │  (Central validation — used by BOTH services)         │         │
│  │  • validateAndResolveCountry() — ISO2/ISO3 lookup     │         │
│  │  • validateCurrencyForCountry() — backend-controlled  │         │
│  │  • validateProviderForCountry() — multi-provider      │         │
│  │  • validateAmount() — positive check                  │         │
│  │  • validatePhoneNumber() — format check               │         │
│  │  • validateAll() — single-call convenience            │         │
│  └────────────────────────────────────────────────────────┘         │
│                                                                      │
│  DepositService  │  PayoutService                                    │
│  (routes to      │  (routes to CountryValidationService)            │
│   CountryValidation + transforms to PawaPay format)                  │
└─────────┬──────────────────┬─────────────────────────────────────────┘
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

| Country | ISO2 | ISO3 | Currency | Supported Providers |
|---------|------|------|----------|-------------------|
| Uganda | `UG` | `UGA` | `UGX` | `MTN_MOMO_UGA`, `AIRTEL_UGA` |
| Zambia | `ZM` | `ZMB` | `ZMW` | `MTN_MOMO_ZMB`, `AIRTEL_ZMB` |
| Rwanda | `RW` | `RWA` | `RWF` | `MTN_MOMO_RWA`, `AIRTEL_RWA` |
| Tanzania | `TZ` | `TZA` | `TZS` | `AIRTEL_TZA`, `VODACOM_TZA`, `TIGO_TZA`, `HALOTEL_TZA` |
| Kenya | `KE` | `KEN` | `KES` | `MPESA_KE`, `AIRTEL_KE`, `TKASH_KE` |
| Nigeria | `NG` | `NGA` | `NGN` | `MTN_MOMO_NG`, `AIRTEL_NG`, `GLO_NG`, `9MOBILE_NG` |
| South Africa | `ZA` | `ZAF` | `ZAR` | `VODACOM_ZA`, `MTN_ZA`, `TELKOM_ZA` |
| Cameroon | `CM` | `CMR` | `XAF` | `MTN_MOMO_CMR`, `ORANGE_CMR` |
| Benin | `BJ` | `BEN` | `XOF` | `MTN_MOMO_BEN`, `MOOV_BEN` |

**Note:** Both ISO2 (e.g. `ZM`) and ISO3 (e.g. `ZMB`) codes are accepted for all countries.

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

**Naming Convention:** `merchantTransactionId` must start with `DEP-` (e.g., `DEP-001`, `DEP-002`)

**Request Body (Zambia):**
```json
{
  "merchantTransactionId": "DEP-001",
  "customerName": "Eniola",
  "phoneNumber": "260763456789",
  "country": "ZMB",
  "currency": "ZMW",
  "amount": 20,
  "provider": "MTN_MOMO_ZMB"
}
```

**Request Body (Nigeria):**
```json
{
  "merchantTransactionId": "DEP-002",
  "customerName": "Chidi",
  "phoneNumber": "2348012345678",
  "country": "NG",
  "currency": "NGN",
  "amount": 5000,
  "provider": "MTN_MOMO_NG"
}
```

**Request Body (Kenya):**
```json
{
  "merchantTransactionId": "DEP-003",
  "customerName": "Wanjiku",
  "phoneNumber": "254712345678",
  "country": "KE",
  "currency": "KES",
  "amount": 1000,
  "provider": "MPESA_KE"
}
```

### 2. Backend validation flow

```
Request received
    │
    ▼
CountryValidationService.validateAll()
    │
    ├── validateAndResolveCountry()   → SupportedCountry lookup (ISO2/ISO3)
    ├── validateCurrencyForCountry()  → Backend-controlled check
    ├── validateProviderForCountry()  → Multi-provider validation
    ├── validateAmount()              → Positive check
    └── validatePhoneNumber()         → 7-15 digit format
    │
    ▼
Backend uses its own currency from SupportedCountry (client currency 
only validated for consistency — backend always controls the final value)
```

### 3. Backend transforms to PawaPay v2 format
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
  "clientReferenceId": "DEP-001",
  "customerMessage": "Deposit DEP-001"
}
```

### 4. Response to client
```json
{
  "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
  "merchantTransactionId": "DEP-001",
  "customerName": "Eniola",
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "ACCEPTED",
  "amount": 20,
  "currency": "ZMW",
  "phoneNumber": "260763456789",
  "country": "ZM",
  "provider": "MTN_MOMO_ZMB",
  "createdAt": "2026-06-10T10:19:43.697"
}
```

---

## 📦 Payout Flow

### 1. Client sends POST `/api/payouts`

**Naming Convention:** `merchantTransactionId` must start with `PAY-` (e.g., `PAY-001`, `PAY-002`)

**Request Body (Tanzania):**
```json
{
  "customerName": "Juma",
  "merchantTransactionId": "PAY-001",
  "phoneNumber": "255712345678",
  "country": "TZ",
  "amount": "15000",
  "currency": "TZS",
  "provider": "VODACOM_TZA"
}
```

**Request Body (South Africa):**
```json
{
  "customerName": "Thabo",
  "merchantTransactionId": "PAY-002",
  "phoneNumber": "278212345678",
  "country": "ZA",
  "amount": "500",
  "currency": "ZAR",
  "provider": "VODACOM_ZA"
}
```

### 2. Backend transforms to PawaPay v2 format
```json
{
  "payoutId": "uuid-generated",
  "recipient": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "255712345678",
      "provider": "VODACOM_TZA"
    }
  },
  "amount": "15000",
  "currency": "TZS",
  "clientReferenceId": "PAY-001",
  "customerMessage": "Payout PAY-001"
}
```

### 3. Response to client
```json
{
  "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
  "merchantTransactionId": "PAY-001",
  "customerName": "Juma",
  "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
  "type": "PAYOUT",
  "status": "ACCEPTED",
  "amount": 15000,
  "currency": "TZS",
  "phoneNumber": "255712345678",
  "country": "TZ",
  "provider": "VODACOM_TZA",
  "createdAt": "2026-06-10T10:19:43.697"
}
```

---

## 🔄 Status Check

### GET `/api/deposits/{transactionId}` or GET `/api/payouts/{transactionId}`

Response:
```json
{
  "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
  "merchantTransactionId": "DEP-001",
  "customerName": "Eniola",
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "COMPLETED",
  "amount": 20,
  "currency": "ZMW",
  "phoneNumber": "260763456789",
  "country": "ZM",
  "provider": "MTN_MOMO_ZMB",
  "createdAt": "2026-06-10T10:19:43.697"
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
  "success": true,
  "message": "Deposit completed successfully",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "duplicate": false,
  "timestamp": "2026-06-16T22:00:00"
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
| **400** | ❌ Bad Request — Validation error (invalid country, currency, provider) |
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
    "merchantTransactionId": "DEP-001",
    "customerName": "Eniola",
    "phoneNumber": "260763456789",
    "country": "ZMB",
    "currency": "ZMW",
    "amount": 20,
    "provider": "MTN_MOMO_ZMB"
  }'
```

**Deposit — Kenya (KES / MPESA):**
```bash
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "merchantTransactionId": "DEP-002",
    "customerName": "Wanjiku",
    "phoneNumber": "254712345678",
    "country": "KE",
    "currency": "KES",
    "amount": 1000,
    "provider": "MPESA_KE"
  }'
```

**Deposit — Nigeria (NGN):**
```bash
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{
    "merchantTransactionId": "DEP-003",
    "customerName": "Chidi",
    "phoneNumber": "2348012345678",
    "country": "NG",
    "currency": "NGN",
    "amount": 5000,
    "provider": "MTN_MOMO_NG"
  }'
```

**Payout — Tanzania (TZS / Vodacom):**
```bash
curl -X POST http://localhost:8080/api/payouts \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Juma",
    "merchantTransactionId": "PAY-001",
    "phoneNumber": "255712345678",
    "country": "TZ",
    "amount": "15000",
    "currency": "TZS",
    "provider": "VODACOM_TZA"
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

### Test 1: Initiate Deposit (any supported country)
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

### Test 5: Initiate Payout (any supported country)
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
│   ├── CountryValidationService.java    # ⬅ NEW — Central validation hub
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
│   ├── SupportedCountry.java            # ⬅ REFACTORED — Multi-country, multi-provider
│   └── Transaction.java                 # Transaction domain model
├── store/
│   └── TransactionStore.java            # In-memory ConcurrentHashMap store
└── exception/
    └── GlobalExceptionHandler.java      # Global error handler
```

---

## 🔑 Key Design Decisions (v2.0 Refactor)

| Decision | Rationale |
|----------|-----------|
| **CountryValidationService** | Centralizes all validation logic — used by BOTH DepositService and PayoutService. Eliminates duplicate validation. |
| **Backend-controlled currency** | Currency is always derived from SupportedCountry enum. Client's currency input is validated for consistency only. |
| **Multi-provider per country** | `SupportedCountry` now holds a `List<String>` of providers instead of a single string. Adding a provider is a one-line change. |
| **Enum-based country resolution** | `SupportedCountry.findByCountryCode()` is the ONLY entry point for country lookup. No hardcoded country checks anywhere. |
| **Extensible by design** | Adding a new country = adding one enum constant. No service/controller changes needed. |
| **validateAll() convenience** | Single method performs all 6 validation steps and returns a `ValidationResult` record. |
| **No business logic in DTOs** | DTOs only have validation annotations. All business rules are in CountryValidationService. |
| **Descriptive error messages** | Errors include the country name, expected currency, and list of valid providers for the country. |

### Why CountryValidationService instead of inline validation?

Before (PayoutService had inline validation):
```java
// ❌ Duplicate logic — also exists in DepositService
if (!supported.getCurrency().equals(currency)) {
    throw new RuntimeException("...");
}
if (!supported.getProvider().equals(provider)) {
    throw new RuntimeException("...");
}
```

After (both services use shared validation):
```java
// ✅ Single source of truth
CountryValidationService.ValidationResult result = countryValidationService.validateAll(
    countryCode, clientCurrency, provider, amount, phoneNumber
);
```

---

## ✅ Files Modified (v2.0 Multi-Country Refactor)

### New Files
| File | Description |
|------|-------------|
| `service/CountryValidationService.java` | Central validation hub — country resolution, currency validation (backend-controlled), multi-provider validation, amount/phone checks. Includes `validateAll()` convenience method. |

### Modified Files
| File | Change |
|------|--------|
| `model/SupportedCountry.java` | **Major refactor.** Added 5 new countries (Rwanda, Tanzania, Kenya, Nigeria, South Africa). Changed from single `String provider` to `List<String> providers` for multi-provider support. Added `isValidProvider()`, `resolveToIso2()`, `resolveToIso3()`. Better error messages listing all countries + providers. |
| `service/DepositService.java` | Refactored to use `CountryValidationService.validateAll()` instead of inline validation. Cleaner extraction of `ValidationResult` (country, validatedProvider, backendCurrency). |
| `service/PayoutService.java` | **Major refactor.** Removed all inline validation logic. Now uses `CountryValidationService` exactly like DepositService. Stores normalized ISO2 country, backend-controlled currency, validated provider. |
| `dto/DepositRequest.java` | Added missing `currency` field (it was being read by DepositService but didn't exist in the DTO). |

### Unchanged Files
| File | Reason |
|------|--------|
| `DepositController.java` | API contract is stable. No changes needed. |
| `PayoutController.java` | API contract is stable. No changes needed. |
| `WebhookService.java` | Unrelated to country support. |
| `Transaction.java` | Model is generic enough. |
| `TransactionStore.java` | Store is country-agnostic. |
| `PawapayClient.java` | Client is country-agnostic. |
| `PawapayProperties.java` | Configuration is country-agnostic. |
| `application.yaml` | No config changes needed for country support. |
| All config files | Unrelated to country support. |

---

## 🐞 Troubleshooting

| Problem | Solution |
|---------|----------|
| `"Unsupported country code: 'XX'"` | Use a supported ISO2 or ISO3 code. See the Supported Countries table above. |
| `"Invalid currency"` | Currency must match the country's backend-controlled currency. E.g., `ZMW` for Zambia, `KES` for Kenya. |
| `"Invalid provider"` | Provider must be in the country's allowed list. E.g., for Zambia: `MTN_MOMO_ZMB` or `AIRTEL_ZMB`. |
| `"Transaction not found"` | Use the `transactionId` from the create response, not `depositId`/`payoutId`. |
| `"Phone number must be 7-15 digits"` | Phone numbers must be numeric (MSISDN format, no `+` or spaces). |
| Port 8080 in use | Kill existing process or change port in `application.yaml`. |
| Webhook failing | Ensure ngrok is running and URL is correct. |
| PawaPay returning errors | Check API key and sandbox URL. |
| **Adding a new country** | Only edit `SupportedCountry.java` — add a new enum constant. No other files need changes. |