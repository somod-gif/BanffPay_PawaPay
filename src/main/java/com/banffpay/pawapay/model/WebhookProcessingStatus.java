package com.banffpay.pawapay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Strongly typed enum for webhook event processing status.
 */
public enum WebhookProcessingStatus {

    PENDING("PENDING"),
    PROCESSED("PROCESSED"),
    FAILED("FAILED"),
    DUPLICATE("DUPLICATE");

    private final String value;

    WebhookProcessingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WebhookProcessingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (WebhookProcessingStatus status : WebhookProcessingStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid webhook processing status: '" + value +
                "'. Allowed values: PENDING, PROCESSED, FAILED, DUPLICATE");
    }

    @Override
    public String toString() {
        return value;
    }
}