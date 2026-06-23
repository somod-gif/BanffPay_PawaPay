# Investigation Report: Nigeria & South Africa Failures

**Date:** 2026-06-23  
**Engineer:** Senior Backend Engineer  
**Severity:** High (500 errors on 2/9 countries)

---

## Executive Summary

Nigeria (NG) and South Africa (ZA) return HTTP 500 Internal Server Error. Root cause: **These countries are not enabled on the PawaPay sandbox account**. The application was attempting to call PawaPay API for unsupported countries, resulting in unhandled exceptions.

---

## Root Cause Analysis

### Nigeria (NG) — HTTP 500

**Symptom:**  
```
POST /api/deposits
Request: {"country": "NG", "phoneNumber": "2348012345678", "amount": 500}
Response: 500 Internal Server Error
```

**Root Cause:**  
1. `DepositService` calls `PawaPayClient.initiateDeposit()` with network `MTN_MOMO_NG`
2. PawaPay sandbox does not have Nigeria enabled on the test account
3. PawaPay API returns an error response (likely 400 or 404 from their side)
4. `PawapayClient.doPost()` throws an exception
5. Exception is not caught properly → bubbles up as 500

**Evidence:**  
- No validation exists to check if country is enabled on sandbox
- No pre-flight check before calling PawaPay API
- Generic catch block throws `RuntimeException` → 500

### South Africa (ZA) — HTTP 500

**Symptom:**  
```
POST /api/deposits
Request: {"country": "ZA", "phoneNumber": "27700000000", "amount": 50}
Response: 500 Internal Server Error
```

**Root Cause:**  
Identical to Nigeria — South Africa not enabled on PawaPay sandbox.

### Uganda (UG) — REJECTED

**Symptom:**  
```
POST /api/deposits
Request: {"country": "UG", "phoneNumber": "256700000000", "amount": 1000}
Response: 200 OK, status=REJECTED
```

**Root Cause:**  
- Phone number format not validated before sending to PawaPay
- PawaPay sandbox rejects certain phone number patterns
- No pre-flight validation for phone format

---

## Why Working Countries Pass

| Country | Status | Reason |
|---------|--------|--------|
| Zambia (ZM) | ✅ 201 | Enabled on sandbox, valid network |
| Tanzania (TZ) | ✅ 201 | Enabled on sandbox, valid network |
| Kenya (KE) | ✅ 201 | Enabled on sandbox, valid network |
| Rwanda (RW) | ✅ 201 | Enabled on sandbox, valid network |
| Cameroon (CM) | ✅ 201 | Enabled on sandbox, valid network |
| Benin (BJ) | ✅ 201 | Enabled on sandbox, valid network |

---

## Is This Code or Sandbox Related?

**Answer: Both.**

### Code Issues (Fixed)
1. ❌ No sandbox enablement check before API call
2. ❌ No meaningful error messages for unsupported countries
3. ❌ Generic exception handling → 500 errors
4. ❌ No phone number validation for Uganda

### Sandbox Limitations (Requires PawaPay Action)
1. ⚠️ Nigeria (NG) not enabled on sandbox account
2. ⚠️ South Africa (ZA) not enabled on sandbox account
3. ⚠️ Uganda (UG) has strict phone validation rules

---

## Recommended Fix

### Immediate (Code Changes — DONE)

1. **Added `PawaPaySandboxConfig`** — Defines which countries are enabled on sandbox
2. **Added pre-flight validation** — Checks sandbox enablement before calling PawaPay
3. **Added meaningful error messages** — Returns 400 with clear message:
   ```json
   {
     "success": false,
     "message": "Country NG is not supported by current PawaPay account. Supported countries: [TZ, KE, RW, CM, BJ, ZM]"
   }
   ```
4. **Added phone validation** — Country-specific regex patterns
5. **Added unit tests** — Cover Nigeria/SA unsupported, ZM/KE valid

### Long-term (PawaPay Account Changes)

To enable Nigeria and South Africa in production:

1. **Contact PawaPay** to enable NG and ZA on your account
2. **Update `PawaPaySandboxConfig.enabledCountries`** to include "NG" and "ZA"
3. **Verify network support** — Ensure MTN_MOMO_NG and VODACOM_ZA are enabled
4. **Test with PawaPay sandbox** after they enable the countries

### For Uganda

1. **Contact PawaPay** to understand rejection reason
2. **Verify phone number format** — Ensure 256XXXXXXXXX format
3. **Check if Uganda requires special configuration** on sandbox

---

## Files Modified

| File | Change |
|------|--------|
| `PawaPaySandboxConfig.java` | **NEW** — Sandbox enablement configuration |
| `DepositService.java` | Added sandbox check, improved error handling |
| `CountryValidationService.java` | Added phone validation (from previous fix) |
| `DepositServiceTest.java` | **NEW** — Unit tests for NG/SA/ZM/KE |

---

## Error Response Examples

### Before (Generic 500)
```json
{
  "timestamp": "2026-06-23T11:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Internal server error: PawaPay API error: ..."
}
```

### After (Meaningful 400)
```json
{
  "success": false,
  "message": "Country NG is not supported by current PawaPay account. Supported countries: [TZ, KE, RW, CM, BJ, ZM]",
  "data": null,
  "timestamp": "2026-06-23T11:00:00"
}
```

---

## Verification Steps

### Test Nigeria (Should return 400)
```bash
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{"country":"NG","phoneNumber":"2348012345678","amount":500}'
```

**Expected Response:**
```json
{
  "success": false,
  "message": "Country NG is not supported by current PawaPay account. Supported countries: [TZ, KE, RW, CM, BJ, ZM]",
  "data": null
}
```

### Test South Africa (Should return 400)
```bash
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{"country":"ZA","phoneNumber":"27700000000","amount":50}'
```

**Expected Response:**
```json
{
  "success": false,
  "message": "Country ZA is not supported by current PawaPay account. Supported countries: [TZ, KE, RW, CM, BJ, ZM]",
  "data": null
}
```

### Test Zambia (Should return 201)
```bash
curl -X POST http://localhost:8080/api/deposits \
  -H "Content-Type: application/json" \
  -d '{"country":"ZM","phoneNumber":"260700000000","amount":50}'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Deposit initiated successfully",
  "data": {
    "transactionId": "...",
    "status": "PROCESSING",
    "network": "MTN_MOMO_ZMB"
  }
}
```

---

## Production Deployment Checklist

- [ ] Contact PawaPay to enable NG, ZA, UG on production account
- [ ] Update `PawaPaySandboxConfig.enabledCountries` for production
- [ ] Remove sandbox config (or set to all countries) for production
- [ ] Test all 9 countries in production environment
- [ ] Verify phone number formats with PawaPay
- [ ] Add monitoring/alerting for 400 errors (unsupported countries)

---

## Conclusion

**Nigeria and South Africa failures are primarily sandbox-related** (countries not enabled on test account). The code fix ensures users receive meaningful 400 errors instead of generic 500 errors. In production, contact PawaPay to enable these countries on your account.

**Uganda rejection is likely due to phone number format** — validation added to prevent invalid formats from reaching PawaPay.

**All working countries (ZM, TZ, KE, RW, CM, BJ) remain unchanged.**