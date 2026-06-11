package com.banffpay.pawapay;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.PayoutRequest;
import com.banffpay.pawapay.dto.TransactionResponse;
import com.banffpay.pawapay.model.SupportedCountry;
import com.banffpay.pawapay.service.CountryValidationService;
import com.banffpay.pawapay.service.PayoutService;
import com.banffpay.pawapay.store.TransactionStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PayoutService}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Successful payouts for Zambia and Uganda</li>
 *   <li>Validation failures: invalid country, invalid currency, wrong currency, invalid provider</li>
 *   <li>Status check with live sync from PawaPay</li>
 * </ul>
 *
 * <p>Validation is delegated to {@link CountryValidationService}, which is mocked
 * to isolate the payout service logic.</p>
 */
@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private PawapayClient pawapayClient;

    @Mock
    private TransactionStore store;

    @Mock
    private CountryValidationService countryValidationService;

    @InjectMocks
    private PayoutService payoutService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PayoutRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new PayoutRequest();
        validRequest.setMerchantTransactionId("INV-673476476");
        validRequest.setCustomerName("Jane Doe");
        validRequest.setPhoneNumber("260763456789");
        validRequest.setCountry("ZM");
        validRequest.setCurrency("ZMW");
        validRequest.setAmount("15");
        validRequest.setProvider("MTN_MOMO_ZMB");
    }

    @Test
    void initiatePayout_zambia_success() throws Exception {
        // Mock validation to succeed
        CountryValidationService.ValidationResult validationResult =
                new CountryValidationService.ValidationResult(
                        SupportedCountry.ZAMBIA, "MTN_MOMO_ZMB", "ZMW");
        when(countryValidationService.validateAll(eq("ZM"), eq("ZMW"), eq("MTN_MOMO_ZMB"),
                eq(new BigDecimal("15")), eq("260763456789")))
                .thenReturn(validationResult);

        JsonNode response = objectMapper.readTree(
                "{\"payoutId\":\"c6601bd2-1568-4140-bf2d-eb77d2b2b222\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiatePayout(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse result = payoutService.initiatePayout(validRequest);

        assertNotNull(result);
        assertEquals("PAYOUT", result.getType());
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals("ZMW", result.getCurrency());
        assertEquals("INV-673476476", result.getMerchantTransactionId());
        assertEquals("Jane Doe", result.getCustomerName());
        assertEquals(BigDecimal.valueOf(15), result.getAmount());
        assertNotNull(result.getTransactionId());
        assertNotNull(result.getPawapayId());
        assertNotNull(result.getCreatedAt());

        // Verify the payout was saved to the store
        verify(store, times(1)).save(any());
        // Verify PawaPay API was called with backend-controlled currency
        verify(pawapayClient).initiatePayout(anyString(), anyString(), anyString(),
                anyString(), eq("ZMW"), anyString(), anyString());
    }

    @Test
    void initiatePayout_invalidCountry() {
        validRequest.setCountry("US");

        // Mock validation to throw exception for invalid country
        when(countryValidationService.validateAll(eq("US"), anyString(), anyString(),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException("Unsupported country code: 'US'"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Unsupported country"));
    }

    @Test
    void initiatePayout_invalidCurrency() {
        validRequest.setCurrency("XYZ");

        // Mock validation to throw exception for invalid currency
        when(countryValidationService.validateAll(eq("ZM"), eq("XYZ"), anyString(),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid currency 'XYZ' for country ZM (Zambia). Expected: ZMW"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency"));
    }

    @Test
    void initiatePayout_wrongCurrencyForCountry() {
        validRequest.setCurrency("UGX");

        // Mock validation to throw exception for wrong currency
        when(countryValidationService.validateAll(eq("ZM"), eq("UGX"), anyString(),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid currency 'UGX' for country ZM (Zambia). Expected: ZMW"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency"));
    }

    @Test
    void initiatePayout_invalidProvider() {
        validRequest.setProvider("WRONG_PROVIDER");

        // Mock validation to throw exception for invalid provider
        when(countryValidationService.validateAll(eq("ZM"), anyString(), eq("WRONG_PROVIDER"),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid provider 'WRONG_PROVIDER' for country ZM (Zambia). Valid providers: MTN_MOMO_ZMB, AIRTEL_ZMB"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Invalid provider"));
    }

    @Test
    void initiatePayout_uganda_success() throws Exception {
        validRequest.setCountry("UG");
        validRequest.setCurrency("UGX");
        validRequest.setProvider("MTN_MOMO_UGA");
        validRequest.setPhoneNumber("256700123456");

        // Mock validation to succeed
        CountryValidationService.ValidationResult ugValidationResult =
                new CountryValidationService.ValidationResult(
                        SupportedCountry.UGANDA, "MTN_MOMO_UGA", "UGX");
        when(countryValidationService.validateAll(eq("UG"), eq("UGX"), eq("MTN_MOMO_UGA"),
                eq(new BigDecimal("15")), eq("256700123456")))
                .thenReturn(ugValidationResult);

        JsonNode response = objectMapper.readTree(
                "{\"payoutId\":\"b5501bd2-1568-4140-bf2d-eb77d2b2b111\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiatePayout(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse result = payoutService.initiatePayout(validRequest);

        assertNotNull(result);
        assertEquals("PAYOUT", result.getType());
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals("UGX", result.getCurrency());

        // Verify PawaPay was called with UGX
        verify(pawapayClient).initiatePayout(anyString(), anyString(), anyString(),
                anyString(), eq("UGX"), anyString(), anyString());
    }

    @Test
    void getPayoutStatus_success() throws Exception {
        String transactionId = "test-tx-id";
        com.banffpay.pawapay.model.Transaction tx = com.banffpay.pawapay.model.Transaction.builder()
                .transactionId(transactionId)
                .merchantTransactionId("INV-673476476")
                .customerName("Jane Doe")
                .pawapayId("c6601bd2-1568-4140-bf2d-eb77d2b2b222")
                .type("PAYOUT")
                .status("PROCESSING")
                .amount(BigDecimal.valueOf(15))
                .currency("ZMW")
                .provider("MTN_MOMO_ZMB")
                .build();

        when(store.findById(transactionId)).thenReturn(Optional.of(tx));

        JsonNode response = objectMapper.readTree("{\"status\":\"COMPLETED\"}");
        when(pawapayClient.checkPayoutStatus("c6601bd2-1568-4140-bf2d-eb77d2b2b222"))
                .thenReturn(response);

        TransactionResponse result = payoutService.getPayoutStatus(transactionId);

        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void getPayoutStatus_transactionNotFound() {
        String transactionId = "non-existent";
        when(store.findById(transactionId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payoutService.getPayoutStatus(transactionId));
        assertTrue(ex.getMessage().contains("Transaction not found"));
    }
}