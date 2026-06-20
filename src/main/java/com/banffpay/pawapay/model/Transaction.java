package com.banffpay.pawapay.model;

import com.banffpay.pawapay.dto.PawapayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction domain model (POJO).
 * Stored in-memory via TransactionStore for development/demo purposes.
 * In production, this would be a JPA entity persisted to a database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private String transactionId;       // internal generated UUID
    private String merchantTransactionId; // client's reference (e.g. INV-123456)
    private String customerName;        // customer name
    private String pawapayId;           // PawaPay's id (depositId or payoutId)
    private TransactionType type;       // DEPOSIT or PAYOUT
//    private TransactionStatus status;   // ACCEPTED, PROCESSING, COMPLETED, FAILED, REJECTED, CANCELLED
    PawapayStatus status;
    private BigDecimal amount;
    private String currency;
    private String phoneNumber;
    private String country;
    private String provider;
    private LocalDateTime createdAt;

    /**
     * Convenience constructor for creating new transactions.
     */
    public Transaction(String transactionId, String merchantTransactionId, String customerName,
                       String pawapayId, TransactionType type, PawapayStatus status,
                       BigDecimal amount, String currency, String phoneNumber,
                       String country, String provider) {
        this.transactionId = transactionId;
        this.merchantTransactionId = merchantTransactionId;
        this.customerName = customerName;
        this.pawapayId = pawapayId;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.provider = provider;
        this.createdAt = LocalDateTime.now();
    }
}