package com.banffpay.pawapay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transaction response returned to client")
public class TransactionResponse {

    @Schema(description = "Internal transaction ID", example = "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02")
    private String transactionId;

    @Schema(description = "Client's unique transaction reference", example = "INV-260763456789")
    private String merchantTransactionId;

    @Schema(description = "Customer name", example = "Eniola")
    private String customerName;

    @Schema(description = "PawaPay transaction ID", example = "f4401bd2-1568-4140-bf2d-eb77d2b2b639")
    private String pawapayId;

    @Schema(description = "Transaction type", example = "DEPOSIT", allowableValues = {"DEPOSIT", "PAYOUT"})
    private String type;

    @Schema(description = "Transaction status", example = "ACCEPTED", allowableValues = {"ACCEPTED", "PROCESSING", "COMPLETED", "FAILED", "REJECTED", "CANCELLED"})
    private String status;

    @Schema(description = "Transaction amount", example = "20")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "ZMW", allowableValues = {"ZMW", "UGX"})
    private String currency;

    @Schema(description = "Creation timestamp", example = "2026-06-10T10:00:00")
    private LocalDateTime createdAt;
}