package com.banffpay.pawapay.service;

import com.banffpay.pawapay.dto.WebhookDTO;
import com.banffpay.pawapay.model.Transaction;
import com.banffpay.pawapay.model.TransactionStatus;
import com.banffpay.pawapay.model.TransactionType;
import com.banffpay.pawapay.model.WebhookEvent;
import com.banffpay.pawapay.model.WebhookProcessingStatus;
import com.banffpay.pawapay.util.TransactionStore;
import com.banffpay.pawapay.util.WebhookEventStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final TransactionStore transactionStore;
    private final WebhookEventStore webhookEventStore;
    private final ObjectMapper objectMapper;

    public WebhookResult processWebhook(WebhookDTO request, String correlationId) {
        MDC.put("correlationId", correlationId);

        try {
            TransactionType type = TransactionType.fromValue(request.getType());
            TransactionStatus status = TransactionStatus.fromValue(request.getStatus());

            Optional<WebhookEvent> existingEvent = webhookEventStore.findByCorrelationId(correlationId);
            if (existingEvent.isPresent()) {
                log.warn("Duplicate webhook detected. correlationId={} pawapayId={} type={} status={}",
                        correlationId, request.getPawapayId(), type, status);
                return WebhookResult.duplicate(correlationId,
                        "Webhook already processed with correlationId: " + correlationId);
            }

            String eventId = UUID.randomUUID().toString();

            WebhookEvent event = WebhookEvent.builder()
                    .id(eventId)
                    .correlationId(correlationId)
                    .pawapayId(request.getPawapayId())
                    .type(type)
                    .status(status)
                    .rawPayload(serializePayload(request))
                    .processingStatus(WebhookProcessingStatus.PENDING)
                    .retryCount(0)
                    .receivedAt(LocalDateTime.now())
                    .build();

            webhookEventStore.save(event);

            Transaction transaction = transactionStore.findByPawapayId(request.getPawapayId())
                    .orElse(null);

            if (transaction == null) {
                log.warn("Transaction not found for pawapayId={}. Manual reconciliation needed.",
                        request.getPawapayId());
                event.setProcessingStatus(WebhookProcessingStatus.FAILED);
                event.setErrorMessage("Transaction not found for pawapayId: " + request.getPawapayId());
                webhookEventStore.updateStatus(event.getId(), WebhookProcessingStatus.FAILED,
                        "Transaction not found for pawapayId: " + request.getPawapayId());
                return WebhookResult.unmatched(correlationId,
                        "Transaction not found for pawapayId: " + request.getPawapayId());
            }

            event.setTransactionId(transaction.getTransactionId());
            String handlerResult = handleWebhookEvent(transaction, type, status, correlationId, event);
            event.setProcessedAt(LocalDateTime.now());

            log.info("Webhook processed. pawapayId={} type={} status={} correlationId={}",
                    request.getPawapayId(), type, status, correlationId);

            return WebhookResult.success(correlationId, handlerResult);

        } catch (Exception e) {
            log.error("Failed to process webhook. pawapayId={} correlationId={} error={}",
                    request.getPawapayId(), correlationId, e.getMessage());
            return WebhookResult.error(correlationId, "Failed to process webhook: " + e.getMessage());
        } finally {
            MDC.remove("correlationId");
        }
    }

    private String handleWebhookEvent(Transaction transaction, TransactionType type,
                                       TransactionStatus status, String correlationId,
                                       WebhookEvent event) {
        TransactionStatus oldStatus = transaction.getStatus();

        log.info("Webhook event. pawapayId={} type={} oldStatus={} newStatus={}",
                transaction.getPawapayId(), type, oldStatus, status);

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
                log.info("Deposit completed. pawapayId={} transactionId={} amount={} {}",
                        transaction.getPawapayId(), transaction.getTransactionId(),
                        transaction.getAmount(), transaction.getCurrency());
                yield String.format("Deposit %s completed successfully. Amount: %s %s",
                        transaction.getTransactionId(), transaction.getAmount(), transaction.getCurrency());
            }
            case FAILED, REJECTED, CANCELLED -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.warn("Deposit failed. pawapayId={} transactionId={} status={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status);
                yield String.format("Deposit %s %s. Status: %s",
                        transaction.getTransactionId(),
                        status == TransactionStatus.FAILED ? "failed" : "rejected", status);
            }
            case ACCEPTED, PROCESSING -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.info("Deposit pending. pawapayId={} transactionId={} status={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status);
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
                log.info("Payout completed. pawapayId={} transactionId={} amount={} {}",
                        transaction.getPawapayId(), transaction.getTransactionId(),
                        transaction.getAmount(), transaction.getCurrency());
                yield String.format("Payout %s completed successfully. Amount: %s %s",
                        transaction.getTransactionId(), transaction.getAmount(), transaction.getCurrency());
            }
            case FAILED, REJECTED, CANCELLED -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.warn("Payout failed. pawapayId={} transactionId={} status={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status);
                yield String.format("Payout %s %s. Status: %s",
                        transaction.getTransactionId(),
                        status == TransactionStatus.FAILED ? "failed" : "rejected", status);
            }
            case ACCEPTED, PROCESSING -> {
                transaction.setStatus(status);
                transactionStore.save(transaction);
                event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
                log.info("Payout pending. pawapayId={} transactionId={} status={}",
                        transaction.getPawapayId(), transaction.getTransactionId(), status);
                yield String.format("Payout %s is %s. Will be reconciled automatically.",
                        transaction.getTransactionId(), status);
            }
        };
    }

    private String serializePayload(WebhookDTO request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize webhook payload: {}", e.getMessage());
            return String.format("{\"pawapayId\":\"%s\",\"type\":\"%s\",\"status\":\"%s\"}",
                    request.getPawapayId(), request.getType(), request.getStatus());
        }
    }
}