package com.banffpay.pawapay;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.DepositRequest;
import com.banffpay.pawapay.dto.TransactionResponse;
import com.banffpay.pawapay.model.SupportedCountry;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.banffpay.pawapay.service.CountryValidationService;
import com.banffpay.pawapay.service.DepositService;
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

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    @Mock
    private PawapayClient pawapayClient;

    @Mock
    private TransactionStore store;

    @Mock
    private CountryValidationService countryValidationService;

    @InjectMocks
    private DepositService depositService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DepositRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new DepositRequest();
        validRequest.setMerchantTransactionId("INV-260763456789");
        validRequest.setCustomerName("Eniola");
        validRequest.setPhoneNumber("260763456789");
        validRequest.setCountry("ZMB");
        validRequest.setCurrency("ZMW");
        validRequest.setAmount("20");
        validRequest.setProvider("MTN_MOMO_ZMB");
    }

    @Test
    void initiateDeposit_zambia_success() throws Exception {
        CountryValidationService.ValidationResult result =
                new CountryValidationService.ValidationResult(
                        SupportedCountry.ZAMBIA, "MTN_MOMO_ZMB", "ZMW");
        when(countryValidationService.validateAll(eq("ZMB"), eq("ZMW"), eq("MTN_MOMO_ZMB"),
                eq(new BigDecimal("20")), eq("260763456789")))
                .thenReturn(result);

        JsonNode response = objectMapper.readTree(
                "{\"depositId\":\"f4401bd2-1568-4140-bf2d-eb77d2b2b639\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse transactionResult = depositService.initiateDeposit(validRequest);

        assertNotNull(transactionResult);
        assertEquals(TransactionType.DEPOSIT, transactionResult.getType());
        assertEquals(TransactionStatus.ACCEPTED, transactionResult.getStatus());
        assertEquals("ZMW", transactionResult.getCurrency());
        assertEquals("INV-260763456789", transactionResult.getMerchantTransactionId());
        assertEquals("Eniola", transactionResult.getCustomerName());
        assertEquals(BigDecimal.valueOf(20), transactionResult.getAmount());
        assertNotNull(transactionResult.getTransactionId());
        assertNotNull(transactionResult.getPawapayId());
        assertNotNull(transactionResult.getCreatedAt());

        verify(store, times(1)).save(any());
        verify(pawapayClient).initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), eq("ZMW"), anyString(), anyString());
    }

    @Test
    void initiateDeposit_invalidCountry() {
        validRequest.setCountry("US");

        when(countryValidationService.validateAll(eq("US"), anyString(), anyString(),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException("Unsupported country code: 'US'"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Unsupported country"));
    }

    @Test
    void initiateDeposit_invalidCurrency() {
        validRequest.setCurrency("XYZ");

        when(countryValidationService.validateAll(eq("ZMB"), eq("XYZ"), anyString(),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid currency 'XYZ' for country ZM (Zambia). Expected: ZMW"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency"));
    }

    @Test
    void initiateDeposit_wrongCurrencyForCountry() {
        validRequest.setCurrency("UGX");

        when(countryValidationService.validateAll(eq("ZMB"), eq("UGX"), anyString(),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid currency 'UGX' for country ZM (Zambia). Expected: ZMW"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency"));
    }

    @Test
    void initiateDeposit_invalidProvider() {
        validRequest.setProvider("WRONG_PROVIDER");

        when(countryValidationService.validateAll(eq("ZMB"), anyString(), eq("WRONG_PROVIDER"),
                any(), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "Invalid provider 'WRONG_PROVIDER' for country ZM (Zambia). Valid providers: MTN_MOMO_ZMB, AIRTEL_ZMB"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Invalid provider"));
    }

    @Test
    void initiateDeposit_uganda_success() throws Exception {
        validRequest.setCountry("UG");
        validRequest.setCurrency("UGX");
        validRequest.setProvider("MTN_MOMO_UGA");
        validRequest.setPhoneNumber("256700123456");

        CountryValidationService.ValidationResult result =
                new CountryValidationService.ValidationResult(
                        SupportedCountry.UGANDA, "MTN_MOMO_UGA", "UGX");
        when(countryValidationService.validateAll(eq("UG"), eq("UGX"), eq("MTN_MOMO_UGA"),
                eq(new BigDecimal("20")), eq("256700123456")))
                .thenReturn(result);

        JsonNode response = objectMapper.readTree(
                "{\"depositId\":\"a2201bd2-1568-4140-bf2d-eb77d2b2b789\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse transactionResult = depositService.initiateDeposit(validRequest);

        assertNotNull(transactionResult);
        assertEquals(TransactionType.DEPOSIT, transactionResult.getType());
        assertEquals(TransactionStatus.ACCEPTED, transactionResult.getStatus());
        assertEquals("UGX", transactionResult.getCurrency());

        verify(pawapayClient).initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), eq("UGX"), anyString(), anyString());
    }

    @Test
    void initiateDeposit_zambia_shortcode_success() throws Exception {
        validRequest.setCountry("ZM");
        validRequest.setCurrency("ZMW");
        validRequest.setProvider("MTN_MOMO_ZMB");
        validRequest.setPhoneNumber("260763456789");

        CountryValidationService.ValidationResult result =
                new CountryValidationService.ValidationResult(
                        SupportedCountry.ZAMBIA, "MTN_MOMO_ZMB", "ZMW");
        when(countryValidationService.validateAll(eq("ZM"), eq("ZMW"), eq("MTN_MOMO_ZMB"),
                eq(new BigDecimal("20")), eq("260763456789")))
                .thenReturn(result);

        JsonNode response = objectMapper.readTree(
                "{\"depositId\":\"f4401bd2-1568-4140-bf2d-eb77d2b2b639\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse transactionResult = depositService.initiateDeposit(validRequest);

        assertNotNull(transactionResult);
        assertEquals(TransactionStatus.ACCEPTED, transactionResult.getStatus());
        assertEquals("ZMW", transactionResult.getCurrency());
    }

    @Test
    void getDepositStatus_success() throws Exception {
        String transactionId = "test-tx-id";
        Transaction tx = Transaction.builder()
                .transactionId(transactionId)
                .merchantTransactionId("INV-260763456789")
                .customerName("Eniola")
                .pawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639")
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PROCESSING)
                .amount(BigDecimal.valueOf(20))
                .currency("ZMW")
                .provider("MTN_MOMO_ZMB")
                .build();

        when(store.findById(transactionId)).thenReturn(Optional.of(tx));

        JsonNode response = objectMapper.readTree("{\"status\":\"COMPLETED\"}");
        when(pawapayClient.checkDepositStatus("f4401bd2-1568-4140-bf2d-eb77d2b2b639"))
                .thenReturn(response);

        TransactionResponse transactionResult = depositService.getDepositStatus(transactionId);

        assertNotNull(transactionResult);
        assertEquals(transactionId, transactionResult.getTransactionId());
        assertEquals(TransactionStatus.COMPLETED, transactionResult.getStatus());
    }

    @Test
    void getDepositStatus_transactionNotFound() {
        String transactionId = "non-existent";
        when(store.findById(transactionId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.getDepositStatus(transactionId));
        assertTrue(ex.getMessage().contains("Transaction not found"));
    }
}