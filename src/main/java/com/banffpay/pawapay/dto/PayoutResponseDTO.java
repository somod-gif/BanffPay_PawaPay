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
 * Strongly typed DTO for payout responses returned to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payout response returned to client")
public class PayoutResponseDTO {

    @Schema(description = "Internal transaction ID", example = "92ad6942-c1c1-48fb-b5a3-ee388db8443f")
    private String transactionId;

    @Schema(description = "Client's unique transaction reference", example = "PAY-001")
    private String merchantTransactionId;

    @Schema(description = "Customer name", example = "Jane Doe")
    private String customerName;

    @Schema(description = "PawaPay payout ID", example = "f60bf205-8d39-444c-9836-a1458eb0d92c")
    private String pawapayId;

    @Schema(description = "Transaction type", example = "PAYOUT")
    private TransactionType type;

    @Schema(description = "Transaction status", example = "ACCEPTED")
    private String status;

    @Schema(description = "Transaction amount", example = "50.00")
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