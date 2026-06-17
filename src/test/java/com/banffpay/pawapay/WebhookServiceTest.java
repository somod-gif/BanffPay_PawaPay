package com.banffpay.pawapay;

import com.banffpay.pawapay.dto.WebhookRequest;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.banffpay.pawapay.service.WebhookResult;
import com.banffpay.pawapay.service.WebhookService;
import com.banffpay.pawapay.store.TransactionStore;
import com.banffpay.pawapay.store.WebhookEventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private TransactionStore transactionStore;

    @Mock
    private WebhookEventStore webhookEventStore;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    private Transaction sampleTransaction;

    private static final String CORRELATION_ID = "test-correlation-id";
    private static final String PAWAPAY_ID = "f4401bd2-1568-4140-bf2d-eb77d2b2b639";

    @BeforeEach
    void setUp() {
        sampleTransaction = Transaction.builder()
                .transactionId("tx-1")
                .merchantTransactionId("INV-123456")
                .customerName("Eniola")
                .pawapayId(PAWAPAY_ID)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PROCESSING)
                .amount(BigDecimal.valueOf(20))
                .currency("ZMW")
                .phoneNumber("260763456789")
                .country("ZMB")
                .provider("MTN_MOMO_ZMB")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void processWebhook_depositCompleted_success() {
        when(transactionStore.findByPawapayId(PAWAPAY_ID))
                .thenReturn(Optional.of(sampleTransaction));
        when(webhookEventStore.findByCorrelationId(CORRELATION_ID))
                .thenReturn(Optional.empty());

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId(PAWAPAY_ID);
        request.setType("DEPOSIT");
        request.setStatus("COMPLETED");

        WebhookResult result = webhookService.processWebhook(request, CORRELATION_ID);

        assertTrue(result.isSuccess());
        assertFalse(result.isDuplicate());
        assertEquals(TransactionStatus.COMPLETED, sampleTransaction.getStatus());
        verify(transactionStore).save(sampleTransaction);
    }

    @Test
    void processWebhook_depositFailed_success() {
        when(transactionStore.findByPawapayId(PAWAPAY_ID))
                .thenReturn(Optional.of(sampleTransaction));
        when(webhookEventStore.findByCorrelationId(CORRELATION_ID))
                .thenReturn(Optional.empty());

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId(PAWAPAY_ID);
        request.setType("DEPOSIT");
        request.setStatus("FAILED");

        WebhookResult result = webhookService.processWebhook(request, CORRELATION_ID);

        assertTrue(result.isSuccess());
        assertEquals(TransactionStatus.FAILED, sampleTransaction.getStatus());
        verify(transactionStore).save(sampleTransaction);
    }

    @Test
    void processWebhook_payoutCompleted_success() {
        Transaction payoutTx = Transaction.builder()
                .transactionId("tx-2")
                .pawapayId(PAWAPAY_ID)
                .type(TransactionType.PAYOUT)
                .status(TransactionStatus.PROCESSING)
                .amount(BigDecimal.valueOf(50))
                .currency("UGX")
                .phoneNumber("256700123456")
                .country("UG")
                .provider("MTN_MOMO_UGA")
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionStore.findByPawapayId(PAWAPAY_ID))
                .thenReturn(Optional.of(payoutTx));
        when(webhookEventStore.findByCorrelationId(CORRELATION_ID))
                .thenReturn(Optional.empty());

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId(PAWAPAY_ID);
        request.setType("PAYOUT");
        request.setStatus("COMPLETED");

        WebhookResult result = webhookService.processWebhook(request, CORRELATION_ID);

        assertTrue(result.isSuccess());
        assertEquals(TransactionStatus.COMPLETED, payoutTx.getStatus());
        verify(transactionStore).save(payoutTx);
    }

    @Test
    void processWebhook_duplicate_idempotent() {
        when(webhookEventStore.findByCorrelationId(CORRELATION_ID))
                .thenReturn(Optional.of(com.banffpay.pawapay.model.WebhookEvent.builder()
                        .id("existing-event")
                        .correlationId(CORRELATION_ID)
                        .build()));

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId(PAWAPAY_ID);
        request.setType("DEPOSIT");
        request.setStatus("COMPLETED");

        WebhookResult result = webhookService.processWebhook(request, CORRELATION_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.isDuplicate());
        // Transaction should NOT have been updated
        assertEquals(TransactionStatus.PROCESSING, sampleTransaction.getStatus());
    }

    @Test
    void processWebhook_unmatchedTransaction() {
        String unknownId = "unknown-id";
        when(webhookEventStore.findByCorrelationId(CORRELATION_ID))
                .thenReturn(Optional.empty());
        when(transactionStore.findByPawapayId(unknownId))
                .thenReturn(Optional.empty());

        WebhookRequest request = new WebhookRequest();
        request.setPawapayId(unknownId);
        request.setType("DEPOSIT");
        request.setStatus("COMPLETED");

        WebhookResult result = webhookService.processWebhook(request, CORRELATION_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.isUnmatched());
        assertTrue(result.getMessage().contains("Transaction not found"));
    }
}