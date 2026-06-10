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

| Country | Code | Currency | Providers |
|---------|------|----------|-----------|
| Zambia | ZM | ZMW | MTN_MOMO_ZMB |
| Uganda | UG | UGX | MTN_MOMO_UGA |

---

## 1. DEPOSIT REQUESTS (POST /api/deposits)

### 1.1 Zambia — MTN MoMo
```json
{
  "depositId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "payer": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "260763456789",
      "provider": "MTN_MOMO_ZMB"
    }
  },
  "amount": "15",
  "currency": "ZMW",
  "clientReferenceId": "INV-123456",
  "customerMessage": "Payment for invoice"
}
```

### 1.2 Uganda — MTN MoMo
```json
{
  "depositId": "a2201bd2-1568-4140-bf2d-eb77d2b2b789",
  "payer": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "256712345678",
      "provider": "MTN_MOMO_UGA"
    }
  },
  "amount": "50000",
  "currency": "UGX",
  "clientReferenceId": "INV-UG-001",
  "customerMessage": "Invoice payment"
}
```

### Expected Success Response (201 Created):
```json
{
  "depositId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "status": "ACCEPTED",
  "created": "2026-06-10T10:00:00Z"
}
```

---

## 2. PAYOUT REQUESTS (POST /api/payouts)

### 2.1 Uganda — MTN MoMo
```json
{
  "payoutId": "b5501bd2-1568-4140-bf2d-eb77d2b2b111",
  "recipient": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "256700123456",
      "provider": "MTN_MOMO_UGA"
    }
  },
  "amount": "100",
  "currency": "UGX",
  "clientReferenceId": "PAYOUT-123",
  "customerMessage": "Salary payment"
}
```

### 2.2 Zambia — MTN MoMo
```json
{
  "payoutId": "c6601bd2-1568-4140-bf2d-eb77d2b2b222",
  "recipient": {
    "type": "MMO",
    "accountDetails": {
      "phoneNumber": "260763456789",
      "provider": "MTN_MOMO_ZMB"
    }
  },
  "amount": "50",
  "currency": "ZMW",
  "clientReferenceId": "PAYOUT-ZM-001",
  "customerMessage": "Refund payment"
}
```

### Expected Success Response (201 Created):
```json
{
  "payoutId": "b5501bd2-1568-4140-bf2d-eb77d2b2b111",
  "status": "ACCEPTED",
  "created": "2026-06-10T10:00:00Z"
}
```

---

## 3. STATUS CHECK

### 3.1 Get Deposit Status
```
GET /api/deposits/{depositId}
```

### 3.2 Get Payout Status
```
GET /api/payouts/{payoutId}
```

The `depositId` or `payoutId` is returned in the response when you create a transaction.

---

## 4. WEBHOOK CALLBACK

### 4.1 Deposit Webhook (PawaPay sends to your app)
```json
{
  "depositId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "status": "COMPLETED",
  "amount": "15",
  "currency": "ZMW"
}
```

### 4.2 Payout Webhook (PawaPay sends to your app)
```json
{
  "payoutId": "b5501bd2-1568-4140-bf2d-eb77d2b2b111",
  "status": "COMPLETED",
  "amount": "100",
  "currency": "UGX"
}
```

---

## 5. PHONE NUMBER FORMAT GUIDE

| Country | Format | Example |
|---------|--------|---------|
| Zambia | 260 + 9 digits | 260763456789 |
| Uganda | 256 + 9 digits | 256712345678 |

**Important:** Phone numbers must be in MSISDN format (digits only, no + sign, no spaces).

---

## 6. TEST ON SWAGGER

1. Start the application: `mvn spring-boot:run`
2. Open Swagger UI: `http://localhost:8080/swagger-ui.html`
3. Click on "Deposit Controller" or "Payout Controller"
4. Click "Try it out"
5. Paste any of the request bodies above
6. Click "Execute"

### Expected Success Response:
```json
{
  "depositId": "f4401bd2-1568-4140-bf2d-eb77d2b2b639",
  "status": "ACCEPTED",
  "created": "2026-06-10T10:00:00Z"
}
```

### Expected Error Response (400 Bad Request):
```json
{
  "success": false,
  "error": "Invalid provider 'WRONG_PROVIDER' for currency ZMW. Expected: MTN_MOMO_ZMB",
  "timestamp": "2026-06-10T10:00:00"
}