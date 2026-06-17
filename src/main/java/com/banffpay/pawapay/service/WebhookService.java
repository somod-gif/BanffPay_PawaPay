package com.banffpay.pawapay.service;

import com.banffpay.pawapay.dto.WebhookRequest;
import com.banffpay.pawapay.model.*;
import com.banffpay.pawapay.store.TransactionStore;
import com.banffpay.pawapay.store.WebhookEventStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Production-grade webhook service with:
 * <ul>
 *   <li>Strongly typed event handling for all 6 transaction scenarios</li>
 *   <li>Idempotency protection via correlation ID deduplication</li>
 *   <li>Structured audit logging with SLF4J MDC</li>
 *   <li>Automatic transaction record updates</li>
 *   <li>Graceful error handling and retry tracking</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final TransactionStore transactionStore;
    private final WebhookEventStore webhookEventStore;
    private final ObjectMapper objectMapper;

    /**
     * Processes an incoming webhook from PawaPay.
     *
     * @param request       the validated webhook request
     * @param correlationId the correlation ID for distributed tracing
     * @return WebhookResult with processing details
     */
    public WebhookResult processWebhook(WebhookRequest request, String correlationId) {
        MDC.put("correlationId", correlationId);
        MDC.put("pawapayId", request.getPawapayId());

        try {
            // Step 1: Resolve enums from validated request strings
            TransactionType type = TransactionType.fromValue(request.getType());
            TransactionStatus status = TransactionStatus.fromValue(request.getStatus());

            // Step 2: Idempotency check via correlation ID
            Optional<WebhookEvent> existingEvent = webhookEventStore.findByCorrelationId(correlationId);
            if (existingEvent.isPresent()) {
                log.warn("Duplicate webhook detected: correlationId={} pawapayId={} type={} status={}",
                        correlationId, request.getPawapayId(), type, status);
                return WebhookResult.duplicate(correlationId,
                        "Webhook already processed with correlationId: " + correlationId);
            }

            // Step 3: Create audit-trail event
            String eventId = UUID.randomUUID().toString();
            String rawPayload = serializePayload(request);

            WebhookEvent event = WebhookEvent.builder()
                    .id(eventId)
                    .correlationId(correlationId)
                    .pawapayId(request.getPawapayId())
                    .type(type)
                    .status(status)
                    .rawPayload(rawPayload)
                    .processingStatus(WebhookProcessingStatus.PENDING)
                    .retryCount(0)
                    .receivedAt(LocalDateTime.now())
                    .build();

            webhookEventStore.save(event);

            // Step 4: Look up the transaction
            Transaction transaction = transactionStore.findByPawapayId(request.getPawapayId())
                    .orElse(null);

            if (transaction == null) {
                log.error("Transaction not found for pawapayId={} type={} status={}. Manual reconciliation needed.",
                        request.getPawapayId(), type, status);
                webhookEventStore.updateStatus(eventId, WebhookProcessingStatus.FAILED,
                        "Transaction not found for pawapayId: " + request.getPawapayId());
                return WebhookResult.unmatched(correlationId,
                        "Transaction not found for pawapayId: " + request.getPawapayId());
            }

            // Step 5: Link event and process
            event.setTransactionId(transaction.getTransactionId());
            String handlerResult = handleWebhookEvent(transaction, type, status, correlationId, event);
            event.setProcessedAt(LocalDateTime.now());

            log.info("[WEBHOOK_PROCESSED] pawapayId={} type={} status={} correlationId={} eventId={}",
                    request.getPawapayId(), type, status, correlationId, eventId);

            return WebhookResult.success(correlationId, handlerResult);

        } catch (Exception e) {
            log.error("Failed to process webhook: pawapayId={} correlationId={} error={}",
                    request.getPawapayId(), correlationId, e.getMessage(), e);
            return WebhookResult.error(correlationId, "Failed to process webhook: " + e.getMessage());
        } finally {
            MDC.remove("correlationId");
            MDC.remove("pawapayId");
        }
    }

    private String handleWebhookEvent(Transaction transaction, TransactionType type,
                                       TransactionStatus status, String correlationId,
                                       WebhookEvent event) {
        TransactionStatus oldStatus = transaction.getStatus();
        log.info("[WEBHOOK_EVENT] pawapayId={} type={} oldStatus={} newStatus={} correlationId={}",
                transaction.getPawapayId(), type, oldStatus, status, correlationId);

        return switch (type) {
            case DEPOSIT -> handleDepositEvent(transaction, status, correlationId, event);
            case PAYOUT -> handlePayoutEvent(transaction, status, correlationId, event);
        };
    }

    private String handleDepositEvent(Transaction transaction, TransactionStatus status,
                                       String correlationId, WebhookEvent event) {
        return switch (status) {
            case COMPLETED -> {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.info("[DEPOSIT_COMPLETED] pawapayId={} transactionId={} amount={} {} correlationId={}",
                        transaction.getPawapayId(), transaction.getTransactionId(),
                        transaction.getAmount(), transaction.getCurrency(), correlationId);
                yield String.format("Deposit %s completed successfully. Amount: %s %s",
                        transaction.getTransactionId(), transaction.getAmount(), transaction.getCurrency());
            }
            case FAILED, REJECTED, CANCELLED -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.warn("[DEPOSIT_FAILED] pawapayId={} transactionId={} status={} correlationId={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status, correlationId);
                yield String.format("Deposit %s %s. Status: %s",
                        transaction.getTransactionId(),
                        status == TransactionStatus.FAILED ? "failed" : "rejected", status);
            }
            case ACCEPTED, PROCESSING -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.info("[DEPOSIT_PENDING] pawapayId={} transactionId={} status={} correlationId={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status, correlationId);
                yield String.format("Deposit %s is %s. Will be reconciled automatically.",
                        transaction.getTransactionId(), status);
            }
        };
    }

    private String handlePayoutEvent(Transaction transaction, TransactionStatus status,
                                      String correlationId, WebhookEvent event) {
        return switch (status) {
            case COMPLETED -> {
                transaction.setStatus(TransactionStatus.COMPLETED);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.info("[PAYOUT_COMPLETED] pawapayId={} transactionId={} amount={} {} correlationId={}",
                        transaction.getPawapayId(), transaction.getTransactionId(),
                        transaction.getAmount(), transaction.getCurrency(), correlationId);
                yield String.format("Payout %s completed successfully. Amount: %s %s",
                        transaction.getTransactionId(), transaction.getAmount(), transaction.getCurrency());
            }
            case FAILED, REJECTED, CANCELLED -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.warn("[PAYOUT_FAILED] pawapayId={} transactionId={} status={} correlationId={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status, correlationId);
                yield String.format("Payout %s %s. Status: %s",
                        transaction.getTransactionId(),
                        status == TransactionStatus.FAILED ? "failed" : "rejected", status);
            }
            case ACCEPTED, PROCESSING -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.info("[PAYOUT_PENDING] pawapayId={} transactionId={} status={} correlationId={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status, correlationId);
                yield String.format("Payout %s is %s. Will be reconciled automatically.",
                        transaction.getTransactionId(), status);
            }
        };
    }

    private String serializePayload(WebhookRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize webhook payload: {}", e.getMessage());
            return String.format("{\"pawapayId\":\"%s\",\"type\":\"%s\",\"status\":\"%s\"}",
                    request.getPawapayId(), request.getType(), request.getStatus());
        }
    }
}