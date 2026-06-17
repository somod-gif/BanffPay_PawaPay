# BanffPay PawaPay API v2 — Request Bodies

## Base URL (Development)
```
http://localhost:8080
```

## Swagger UI
```
http://localhost:8080/swagger-ui.html
```

---

## Supported Countries & Providers (v2)

| Country | ISO2 | ISO3 | Currency | Providers |
|---------|------|------|----------|-----------|
| Uganda | UG | UGA | UGX | MTN_MOMO_UGA, AIRTEL_UGA |
| Zambia | ZM | ZMB | ZMW | MTN_MOMO_ZMB, AIRTEL_ZMB |
| Rwanda | RW | RWA | RWF | MTN_MOMO_RWA, AIRTEL_RWA |
| Tanzania | TZ | TZA | TZS | AIRTEL_TZA, VODACOM_TZA, TIGO_TZA, HALOTEL_TZA |
| Kenya | KE | KEN | KES | MPESA_KE, AIRTEL_KE, TKASH_KE |
| Nigeria | NG | NGA | NGN | MTN_MOMO_NG, AIRTEL_NG, GLO_NG, 9MOBILE_NG |
| South Africa | ZA | ZAF | ZAR | VODACOM_ZA, MTN_ZA, TELKOM_ZA |
| Cameroon | CM | CMR | XAF | MTN_MOMO_CMR, ORANGE_CMR |
| Benin | BJ | BEN | XOF | MTN_MOMO_BEN, MOOV_BEN |

> **Note:** Both ISO2 (e.g. `ZM`) and ISO3 (e.g. `ZMB`) codes are accepted for all countries.
> **Phone numbers:** Must be in MSISDN format (digits only, no `+` sign, no spaces, 7-15 digits).

---

## 1. DEPOSIT REQUESTS (POST /api/deposits)

### 1.1 Zambia — MTN MoMo
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

### 1.2 Uganda — MTN MoMo
```json
{
  "merchantTransactionId": "INV-256712345678",
  "customerName": "Grace",
  "phoneNumber": "256712345678",
  "country": "UG",
  "currency": "UGX",
  "amount": 50000,
  "provider": "MTN_MOMO_UGA"
}
```

### 1.3 Kenya — MPESA
```json
{
  "merchantTransactionId": "INV-254712345678",
  "customerName": "Wanjiku",
  "phoneNumber": "254712345678",
  "country": "KE",
  "currency": "KES",
  "amount": 1000,
  "provider": "MPESA_KE"
}
```

### 1.4 Tanzania — Vodacom
```json
{
  "merchantTransactionId": "INV-255712345678",
  "customerName": "Juma",
  "phoneNumber": "255712345678",
  "country": "TZ",
  "currency": "TZS",
  "amount": 15000,
  "provider": "VODACOM_TZA"
}
```

### 1.5 Nigeria — MTN
```json
{
  "merchantTransactionId": "INV-2345678901",
  "customerName": "Chidi",
  "phoneNumber": "2348012345678",
  "country": "NG",
  "currency": "NGN",
  "amount": 5000,
  "provider": "MTN_MOMO_NG"
}
```

### 1.6 South Africa — Vodacom
```json
{
  "merchantTransactionId": "INV-278212345678",
  "customerName": "Thabo",
  "phoneNumber": "278212345678",
  "country": "ZA",
  "currency": "ZAR",
  "amount": 500,
  "provider": "VODACOM_ZA"
}
```

### 1.7 Rwanda — MTN MoMo
```json
{
  "merchantTransactionId": "INV-250712345678",
  "customerName": "Alice",
  "phoneNumber": "250712345678",
  "country": "RW",
  "currency": "RWF",
  "amount": 10000,
  "provider": "MTN_MOMO_RWA"
}
```

### 1.8 Cameroon — MTN MoMo
```json
{
  "merchantTransactionId": "INV-237678901234",
  "customerName": "Paul",
  "phoneNumber": "237678901234",
  "country": "CM",
  "currency": "XAF",
  "amount": 5000,
  "provider": "MTN_MOMO_CMR"
}
```

### 1.9 Benin — MTN MoMo
```json
{
  "merchantTransactionId": "INV-22990123456",
  "customerName": "Amadou",
  "phoneNumber": "22990123456",
  "country": "BJ",
  "currency": "XOF",
  "amount": 10000,
  "provider": "MTN_MOMO_BEN"
}
```

### Expected Success Response
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
  "phoneNumber": "260763456789",
  "country": "ZM",
  "provider": "MTN_MOMO_ZMB",
  "createdAt": "2026-06-10T10:19:43.697"
}
```

---

## 2. PAYOUT REQUESTS (POST /api/payouts)

### 2.1 Zambia — MTN MoMo
```json
{
  "customerName": "Eniola",
  "merchantTransactionId": "PAYOUT-ZM-001",
  "phoneNumber": "260763456789",
  "country": "ZM",
  "amount": "50",
  "currency": "ZMW",
  "provider": "MTN_MOMO_ZMB"
}
```

### 2.2 Uganda — MTN MoMo
```json
{
  "customerName": "Grace",
  "merchantTransactionId": "PAYOUT-UG-001",
  "phoneNumber": "256700123456",
  "country": "UG",
  "amount": "100",
  "currency": "UGX",
  "provider": "MTN_MOMO_UGA"
}
```

### 2.3 Tanzania — Airtel
```json
{
  "customerName": "Juma",
  "merchantTransactionId": "PAYOUT-TZ-001",
  "phoneNumber": "255712345678",
  "country": "TZ",
  "amount": "25000",
  "currency": "TZS",
  "provider": "AIRTEL_TZA"
}
```

### 2.4 Nigeria — MTN
```json
{
  "customerName": "Chidi",
  "merchantTransactionId": "PAYOUT-NG-001",
  "phoneNumber": "2348012345678",
  "country": "NG",
  "amount": "10000",
  "currency": "NGN",
  "provider": "MTN_MOMO_NG"
}
```

### Expected Success Response
```json
{
  "transactionId": "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02",
  "merchantTransactionId": "PAYOUT-ZM-001",
  "customerName": "Eniola",
  "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
  "type": "PAYOUT",
  "status": "ACCEPTED",
  "amount": 50,
  "currency": "ZMW",
  "phoneNumber": "260763456789",
  "country": "ZM",
  "provider": "MTN_MOMO_ZMB",
  "createdAt": "2026-06-10T10:19:43.697"
}
```

---

## 3. STATUS CHECK

### 3.1 Get Deposit Status
```
GET /api/deposits/{transactionId}
```

### 3.2 Get Payout Status
```
GET /api/payouts/{transactionId}
```

Use the `transactionId` (not `pawapayId`) returned from the create response.

**Response (same format as create):**
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
  "phoneNumber": "260763456789",
  "country": "ZM",
  "provider": "MTN_MOMO_ZMB",
  "createdAt": "2026-06-10T10:19:43.697"
}
```

---

## 4. WEBHOOK CALLBACK

### Endpoint
```
POST /api/webhooks/pawapay
```

### Headers
| Header | Required | Description |
|--------|----------|-------------|
| `X-Correlation-ID` | No (auto-generated if missing) | UUID for idempotency and tracing. Duplicate webhooks with the same correlationId return 200 with `duplicate: true` |
| `Content-Type` | Yes | `application/json` |

### Request Body (Common to all scenarios)
```json
{
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
```

| Field | Type | Allowed Values | Description |
|-------|------|----------------|-------------|
| `pawapayId` | string | UUID | PawaPay's transaction ID (depositId or payoutId) |
| `type` | string | `DEPOSIT`, `PAYOUT` | Transaction type |
| `status` | string | `ACCEPTED`, `PROCESSING`, `COMPLETED`, `FAILED`, `REJECTED`, `CANCELLED` | Transaction status |

---

### 4.1 Deposit Completed
**Request:**
```json
{
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Deposit 7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02 completed successfully. Amount: 20 ZMW",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "duplicate": false,
  "unmatched": false,
  "timestamp": "2026-06-16T22:00:00"
}
```

### 4.2 Deposit Failed
**Request:**
```json
{
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "FAILED"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Deposit 7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02 failed. Status: FAILED",
  "correlationId": "550e8400-e29b-41d4-a716-446655440001",
  "duplicate": false,
  "unmatched": false,
  "timestamp": "2026-06-16T22:01:00"
}
```

### 4.3 Deposit Pending
**Request:**
```json
{
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "PROCESSING"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Deposit 7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02 is PROCESSING. Will be reconciled automatically.",
  "correlationId": "550e8400-e29b-41d4-a716-446655440002",
  "duplicate": false,
  "unmatched": false,
  "timestamp": "2026-06-16T22:02:00"
}
```

### 4.4 Payout Completed
**Request:**
```json
{
  "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
  "type": "PAYOUT",
  "status": "COMPLETED"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payout 7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02 completed successfully. Amount: 50 ZMW",
  "correlationId": "550e8400-e29b-41d4-a716-446655440003",
  "duplicate": false,
  "unmatched": false,
  "timestamp": "2026-06-16T22:03:00"
}
```

### 4.5 Payout Failed
**Request:**
```json
{
  "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
  "type": "PAYOUT",
  "status": "FAILED"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payout 7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02 failed. Status: FAILED",
  "correlationId": "550e8400-e29b-41d4-a716-446655440004",
  "duplicate": false,
  "unmatched": false,
  "timestamp": "2026-06-16T22:04:00"
}
```

### 4.6 Payout Pending
**Request:**
```json
{
  "pawapayId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
  "type": "PAYOUT",
  "status": "PROCESSING"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payout 7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02 is PROCESSING. Will be reconciled automatically.",
  "correlationId": "550e8400-e29b-41d4-a716-446655440005",
  "duplicate": false,
  "unmatched": false,
  "timestamp": "2026-06-16T22:05:00"
}
```

### 4.7 Duplicate Webhook (Idempotent Response)
**Request (same correlationId as a previous call):**
```json
{
  "pawapayId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
```
**Headers:**
```
X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
```
**Response (200 OK — not 409):**
```json
{
  "success": true,
  "message": "Webhook already processed with correlationId: 550e8400-e29b-41d4-a716-446655440000",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "duplicate": true,
  "unmatched": false,
  "timestamp": "2026-06-16T22:06:00"
}
```
> **Note:** Duplicate webhooks return `200 OK` (not 409) because the webhook was already successfully processed. The `duplicate: true` flag signals the sender not to retry.

### 4.8 Unmatched Transaction (Webhook for unknown pawapayId)
**Request:**
```json
{
  "pawapayId": "00000000-0000-0000-0000-000000000000",
  "type": "DEPOSIT",
  "status": "COMPLETED"
}
```
**Response (202 Accepted — pending reconciliation):**
```json
{
  "success": false,
  "message": "Transaction not found for pawapayId: 00000000-0000-0000-0000-000000000000",
  "correlationId": "550e8400-e29b-41d4-a716-446655440006",
  "duplicate": false,
  "unmatched": true,
  "timestamp": "2026-06-16T22:07:00"
}
```
> **Note:** Returns `202 Accepted` — the webhook is stored but the transaction may arrive later. The scheduled reconciliation job will retry these every 5 minutes.

---

## 5. RECONCILIATION (Automatic)

The reconciliation service runs every 5 minutes via `@Scheduled` and:

1. Finds all transactions with pending statuses (`ACCEPTED`, `PROCESSING`) older than 5 minutes
2. Calls PawaPay's status API to check for updates
3. Updates transaction records automatically
4. Logs reconciliation results

**No manual intervention required.** Pending transactions from webhooks with `unmatched: true` are stored and can be manually linked if needed.

---

## 6. ERROR RESPONSES

### Validation Error (400 Bad Request)
```json
{
  "success": false,
  "error": "phoneNumber: Phone number must be 7-15 digits",
  "timestamp": "2026-06-10T10:00:00"
}
```

### Invalid Provider (400 Bad Request)
```json
{
  "success": false,
  "error": "Invalid provider 'WRONG_PROVIDER' for currency ZMW. Expected: MTN_MOMO_ZMB",
  "timestamp": "2026-06-10T10:00:00"
}
```

### Transaction Not Found (400 Bad Request)
```json
{
  "success": false,
  "error": "Transaction not found for pawapayId: f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "timestamp": "2026-06-10T10:00:00"
}
```

### Internal Server Error (500)
```json
{
  "success": false,
  "error": "Internal server error",
  "timestamp": "2026-06-10T10:00:00"
}
```

---

## 7. TESTING FLOW

### Step 1: Initiate a Deposit
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
→ Save `transactionId` and `pawapayId` from the response.

### Step 2: Simulate Webhook — Deposit Completed
```bash
curl -X POST http://localhost:8080/api/webhooks/pawapay \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-correlation-001" \
  -d '{
    "pawapayId": "SAME_PAWAPAY_ID_FROM_STEP_1",
    "type": "DEPOSIT",
    "status": "COMPLETED"
  }'
```

### Step 3: Verify Status Updated
```bash
curl http://localhost:8080/api/deposits/SAME_TRANSACTION_ID_FROM_STEP_1
```
→ Expect `status: "COMPLETED"`

### Step 4: Test Idempotency (Replay same webhook)
```bash
curl -X POST http://localhost:8080/api/webhooks/pawapay \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-correlation-001" \
  -d '{
    "pawapayId": "SAME_PAWAPAY_ID",
    "type": "DEPOSIT",
    "status": "COMPLETED"
  }'
```
→ Expect `duplicate: true`

---

## 8. PHONE NUMBER FORMAT GUIDE

| Country | Code | Format | Example |
|---------|------|--------|---------|
| Zambia | +260 | 260 + 9 digits | 260763456789 |
| Uganda | +256 | 256 + 9 digits | 256712345678 |
| Kenya | +254 | 254 + 9 digits | 254712345678 |
| Tanzania | +255 | 255 + 9 digits | 255712345678 |
| Nigeria | +234 | 234 + 10-11 digits | 2348012345678 |
| South Africa | +27 | 27 + 9-10 digits | 278212345678 |
| Rwanda | +250 | 250 + 9 digits | 250712345678 |
| Cameroon | +237 | 237 + 9 digits | 237678901234 |
| Benin | +229 | 229 + 8 digits | 22990123456 |

> **All phone numbers must be:** digits only, no `+` sign, no spaces, no dashes, 7-15 digits total.