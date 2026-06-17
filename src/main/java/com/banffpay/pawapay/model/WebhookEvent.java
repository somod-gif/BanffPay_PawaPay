package com.banffpay.pawapay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Webhook event domain model for audit logging.
 * Stored in-memory via WebhookEventStore for development/demo purposes.
 * Each event captures every incoming webhook callback for compliance,
 * debugging, reconciliation, and idempotency verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    private String id;

    /**
     * Correlation ID for distributed tracing and idempotency.
     * Must be unique across all webhook events.
     */
    private String correlationId;

    /**
     * PawaPay's transaction ID (depositId or payoutId)
     */
    private String pawapayId;

    /**
     * Internal transaction ID (cross-reference to Transaction)
     */
    private String transactionId;

    /**
     * Transaction type: DEPOSIT or PAYOUT
     */
    private TransactionType type;

    /**
     * Transaction status from webhook
     */
    private TransactionStatus status;

    /**
     * Raw webhook payload for audit trail
     */
    private String rawPayload;

    /**
     * Processing status: PENDING, PROCESSED, FAILED, DUPLICATE
     */
    private WebhookProcessingStatus processingStatus;

    /**
     * Error message if processing failed
     */
    private String errorMessage;

    /**
     * Number of processing attempts
     */
    private Integer retryCount;

    /**
     * Timestamp when webhook was received
     */
    private LocalDateTime receivedAt;

    /**
     * Timestamp when webhook was processed
     */
    private LocalDateTime processedAt;
}