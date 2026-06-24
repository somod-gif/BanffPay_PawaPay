package com.banffpay.pawapay.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction POJO for in-memory storage.
 * No JPA annotations — persistence is handled by TransactionStore.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String id;

    private String transactionId;       // internal generated UUID

    private String merchantTransactionId; // client's reference (e.g. INV-123456)

    private String customerName;        // customer name

    private String pawapayId;           // PawaPay's id (depositId or payoutId)

    private TransactionType type;       // DEPOSIT or PAYOUT

    private TransactionStatus status;   // ACCEPTED, PROCESSING, COMPLETED, FAILED, REJECTED, CANCELLED

    private BigDecimal amount;

    private String currency;

    private String phoneNumber;

    private String country;

    private String provider;

    private LocalDateTime createdAt;

}