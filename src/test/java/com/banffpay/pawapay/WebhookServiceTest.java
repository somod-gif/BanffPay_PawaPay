package com.banffpay.pawapay;

import com.banffpay.pawapay.dto.WebhookRequest;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.service.WebhookService;
import com.banffpay.pawapay.store.TransactionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private TransactionStore store;

    @InjectMocks
    private WebhookService webhookService;

    private Transaction sampleTransaction;

    @BeforeEach
    void setUp() {
        sampleTransaction = Transaction.builder()
                .transactionId("tx-1")
                .merchantTransactionId("INV-123456")
                .customerName("Eniola")
                .pawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639")
                .type("DEPOSIT")
                .status("PROCESSING")
                .amount(BigDecimal.valueOf(20))
                .currency("ZMW")
                .phoneNumber("260763456789")
                .country("ZMB")
                .provider("MTN_MOMO_ZMB")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processWebhook_completed() {
        when(store.findByPawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639"))
                .thenReturn(Optional.of(sampleTransaction));

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639");
        request.setType("DEPOSIT");
        request.setStatus("COMPLETED");

        String result = webhookService.processWebhook(request);

        assertEquals("Webhook processed successfully", result);
        assertEquals("COMPLETED", sampleTransaction.getStatus());
        verify(store).save(sampleTransaction);
    }

    @Test
    void processWebhook_failed() {
        when(store.findByPawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639"))
                .thenReturn(Optional.of(sampleTransaction));

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639");
        request.setType("DEPOSIT");
        request.setStatus("FAILED");

        String result = webhookService.processWebhook(request);

        assertEquals("Webhook processed successfully", result);
        assertEquals("FAILED", sampleTransaction.getStatus());
    }

    @Test
    void processWebhook_invalidStatus() {
        WebhookRequest request = new WebhookRequest();
        request.setPawapayId("f4401bd2-1568-4140-bf2d-eb77d2b2b639");
        request.setType("DEPOSIT");
        request.setStatus("INVALID");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> webhookService.processWebhook(request));
        assertTrue(ex.getMessage().contains("Invalid status"));
    }

    @Test
    void processWebhook_transactionNotFound() {
        when(store.findByPawapayId("unknown-id")).thenReturn(Optional.empty());

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId("unknown-id");
        request.setType("DEPOSIT");
        request.setStatus("COMPLETED");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> webhookService.processWebhook(request));
        assertTrue(ex.getMessage().contains("Transaction not found"));
    }
}