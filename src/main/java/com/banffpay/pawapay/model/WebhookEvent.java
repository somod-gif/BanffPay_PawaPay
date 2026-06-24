package com.banffpay.pawapay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Webhook event POJO for in-memory storage.
 * No JPA annotations — persistence is handled by WebhookEventStore.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    private String id;

    private String correlationId;

    private String pawapayId;

    private String transactionId;

    private TransactionType type;

    private TransactionStatus status;

    private String rawPayload;

    private WebhookProcessingStatus processingStatus;

    private String errorMessage;

    private Integer retryCount;

    private LocalDateTime receivedAt;

    private LocalDateTime processedAt;
}