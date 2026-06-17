package com.banffpay.pawapay.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Strongly typed enum for PawaPay transaction types.
 * Replaces raw String usage for type safety and compile-time validation.
 */
public enum TransactionType {

    DEPOSIT("DEPOSIT"),
    PAYOUT("PAYOUT");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransactionType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (TransactionType type : TransactionType.values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid transaction type: '" + value + "'. Allowed values: DEPOSIT, PAYOUT");
    }

    @Override
    public String toString() {
        return value;
    }
}