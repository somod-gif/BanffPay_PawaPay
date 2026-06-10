package com.banffpay.pawapay;

import com.banffpay.pawapay.client.PawapayClient;
import com.banffpay.pawapay.dto.DepositRequest;
import com.banffpay.pawapay.dto.TransactionResponse;
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
        JsonNode response = objectMapper.readTree(
                "{\"depositId\":\"f4401bd2-1568-4140-bf2d-eb77d2b2b639\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse result = depositService.initiateDeposit(validRequest);

        assertNotNull(result);
        assertEquals("DEPOSIT", result.getType());
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals("ZMW", result.getCurrency());
        assertEquals("INV-260763456789", result.getMerchantTransactionId());
        assertEquals("Eniola", result.getCustomerName());
        assertEquals(BigDecimal.valueOf(20), result.getAmount());
        assertNotNull(result.getTransactionId());
        assertNotNull(result.getPawapayId());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void initiateDeposit_invalidCountry() {
        validRequest.setCountry("US");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Unsupported country"));
    }

    @Test
    void initiateDeposit_invalidCurrency() {
        validRequest.setCurrency("XYZ");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency") || ex.getMessage().contains("Unsupported currency"));
    }

    @Test
    void initiateDeposit_wrongCurrencyForCountry() {
        validRequest.setCurrency("UGX");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Invalid currency"));
    }

    @Test
    void initiateDeposit_invalidProvider() {
        validRequest.setProvider("WRONG_PROVIDER");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> depositService.initiateDeposit(validRequest));
        assertTrue(ex.getMessage().contains("Invalid provider"));
    }

    @Test
    void initiateDeposit_uganda_success() throws Exception {
        validRequest.setCountry("UG");
        validRequest.setCurrency("UGX");
        validRequest.setProvider("MTN_MOMO_UGA");
        validRequest.setPhoneNumber("256700123456");

        JsonNode response = objectMapper.readTree(
                "{\"depositId\":\"a2201bd2-1568-4140-bf2d-eb77d2b2b789\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse result = depositService.initiateDeposit(validRequest);

        assertNotNull(result);
        assertEquals("DEPOSIT", result.getType());
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals("UGX", result.getCurrency());
    }

    @Test
    void initiateDeposit_zambia_shortcode_success() throws Exception {
        validRequest.setCountry("ZM");
        validRequest.setCurrency("ZMW");
        validRequest.setProvider("MTN_MOMO_ZMB");
        validRequest.setPhoneNumber("260763456789");

        JsonNode response = objectMapper.readTree(
                "{\"depositId\":\"f4401bd2-1568-4140-bf2d-eb77d2b2b639\",\"status\":\"ACCEPTED\",\"created\":\"2026-06-10T10:00:00Z\"}");
        when(pawapayClient.initiateDeposit(anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(response);

        TransactionResponse result = depositService.initiateDeposit(validRequest);

        assertNotNull(result);
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals("ZMW", result.getCurrency());
    }

    @Test
    void getDepositStatus_success() throws Exception {
        String transactionId = "test-tx-id";
        com.banffpay.pawapay.model.Transaction tx = com.banffpay.pawapay.model.Transaction.builder()
                .transactionId(transactionId)
                .merchantTransactionId("INV-260763456789")
                .customerName("Eniola")
                .pawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639")
                .type("DEPOSIT")
                .status("PROCESSING")
                .amount(BigDecimal.valueOf(20))
                .currency("ZMW")
                .provider("MTN_MOMO_ZMB")
                .build();

        when(store.findById(transactionId)).thenReturn(Optional.of(tx));

        JsonNode response = objectMapper.readTree("{\"status\":\"COMPLETED\"}");
        when(pawapayClient.checkDepositStatus("f4401bd2-1568-4140-bf2d-eb77d2b2b639"))
                .thenReturn(response);

        TransactionResponse result = depositService.getDepositStatus(transactionId);

        assertNotNull(result);
        assertEquals(transactionId, result.getTransactionId());
        assertEquals("COMPLETED", result.getStatus());
    }
}