package com.banffpay.pawapay.dto;


/**
 * @author Yusuf Olosan
 * @role software engineer
 * @createdOn 20 Sat Jun, 2026
 */

public enum PawapayStatus {
    INVALID_AMOUNT,
    ACCEPTED,
    DUPLICATELIGNORED,
    PROVIDER_TEMPORARILY_UNAVAILABLE,
    INVALID_PHONE_NUMBER,
    INVALID_CURRENCY,
    AMOUNT_OUT_OF_BOUNDS,
    COMPLETED,
    PROCESSING,
    IN_RECONCILIATION,
    FAILED,
    NOT_FOUND
}
