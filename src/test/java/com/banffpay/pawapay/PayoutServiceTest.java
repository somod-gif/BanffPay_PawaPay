package com.banffpay.pawapay;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.PayoutRequest;
import com.banffpay.pawapay.dto.TransactionResponse;
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

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock
    private PawapayClient pawapayClient;

    @Mock
    private TransactionStore store;

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
    }

    @Test
    void initiatePayout_invalidCountry() {
        validRequest.setCountry("US");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Unsupported country"));
    }

    @Test
    void initiatePayout_invalidCurrency() {
        validRequest.setCurrency("XYZ");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency") || ex.getMessage().contains("Unsupported currency"));
    }

    @Test
    void initiatePayout_wrongCurrencyForCountry() {
        validRequest.setCurrency("UGX");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency"));
    }

    @Test
    void initiatePayout_invalidProvider() {
        validRequest.setProvider("WRONG_PROVIDER");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payoutService.initiatePayout(validRequest));
        assertTrue(ex.getMessage().contains("Invalid provider"));
    }

    @Test
    void initiatePayout_uganda_success() throws Exception {
        validRequest.setCountry("UG");
        validRequest.setCurrency("UGX");
        validRequest.setProvider("MTN_MOMO_UGA");
        validRequest.setPhoneNumber("256700123456");

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
}