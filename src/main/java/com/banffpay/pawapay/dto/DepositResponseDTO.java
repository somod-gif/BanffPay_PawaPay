package com.banffpay.pawapay.dto;

import com.banffpay.pawapay.model.TransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Strongly typed DTO for deposit responses returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Deposit response returned to client")
public class DepositResponseDTO {

    @Schema(description = "Internal transaction ID", example = "7c0e94e8-1b7d-4c5c-b1cb-77ef66c99c02")
    private String transactionId;

    @Schema(description = "Client's unique transaction reference", example = "DEP-001")
    private String merchantTransactionId;

    @Schema(description = "Customer name", example = "Jane Doe")
    private String customerName;

    @Schema(description = "PawaPay deposit ID", example = "f4401bd2-1568-4140-bf2d-eb77d2b2b639")
    private String pawapayId;

    @Schema(description = "Transaction type", example = "DEPOSIT")
    private TransactionType type;

    @Schema(description = "Transaction status", example = "ACCEPTED")
    private String status;

    @Schema(description = "Transaction amount", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "ZMW")
    private String currency;

    @Schema(description = "Phone number", example = "260763456789")
    private String phoneNumber;

    @Schema(description = "Country ISO2 code", example = "ZM")
    private String country;

    @Schema(description = "Resolved mobile money network", example = "MTN_MOMO_ZMB")
    private String network;

    @Schema(description = "Creation timestamp", example = "2026-06-22T20:00:00")
    private LocalDateTime createdAt;
}