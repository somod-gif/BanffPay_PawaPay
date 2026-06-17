package com.banffpay.pawapay.service;

import lombok.Builder;
import lombok.Value;

/**
 * Result wrapper for webhook processing responses.
 * Provides structured feedback including idempotency detection.
 */
@Value
@Builder(builderClassName = "Builder")
public class WebhookResult {
    String correlationId;
    String message;
    boolean success;
    boolean duplicate;
    boolean unmatched;

    /**
     * Creates a success result for a processed webhook.
     */
    public static WebhookResult success(String correlationId, String message) {
        return WebhookResult.builder()
                .correlationId(correlationId)
                .message(message)
                .success(true)
                .duplicate(false)
                .unmatched(false)
                .build();
    }

    /**
     * Creates a success result for a duplicate webhook (idempotent response).
     */
    public static WebhookResult duplicate(String correlationId, String message) {
        return WebhookResult.builder()
                .correlationId(correlationId)
                .message(message)
                .success(true)
                .duplicate(true)
                .unmatched(false)
                .build();
    }

    /**
     * Creates a result for a webhook that references an unknown transaction.
     */
    public static WebhookResult unmatched(String correlationId, String message) {
        return WebhookResult.builder()
                .correlationId(correlationId)
                .message(message)
                .success(false)
                .duplicate(false)
                .unmatched(true)
                .build();
    }

    /**
     * Creates an error result for a failed webhook processing.
     */
    public static WebhookResult error(String correlationId, String message) {
        return WebhookResult.builder()
                .correlationId(correlationId)
                .message(message)
                .success(false)
                .duplicate(false)
                .unmatched(false)
                .build();
    }
}