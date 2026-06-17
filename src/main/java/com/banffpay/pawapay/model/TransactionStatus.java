package com.banffpay.pawapay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Strongly typed enum for PawaPay transaction statuses.
 * Replaces raw String usage for type safety and compile-time validation.
 */
public enum TransactionStatus {

    ACCEPTED("ACCEPTED"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED");

    private final String value;

    TransactionStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransactionStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (TransactionStatus status : TransactionStatus.values()) {
            if (status.value.equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid transaction status: '" + value +
                "'. Allowed values: ACCEPTED, PROCESSING, COMPLETED, FAILED, REJECTED, CANCELLED");
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Checks if this status represents a terminal (non-recoverable) state.
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == REJECTED || this == CANCELLED;
    }

    /**
     * Checks if this status represents a successful transaction.
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Checks if this status represents a failed transaction.
     */
    public boolean isFailed() {
        return this == FAILED || this == REJECTED || this == CANCELLED;
    }

    /**
     * Checks if this status represents a pending/in-progress transaction.
     */
    public boolean isPending() {
        return this == ACCEPTED || this == PROCESSING;
    }
}